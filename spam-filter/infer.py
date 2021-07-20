from tensorflow import keras
import tensorflow as tf
from kafka import KafkaConsumer, KafkaProducer
from tensorflow.keras.preprocessing.sequence import pad_sequences
from spacy.tokenizer import Tokenizer
from spacy.lang.en import English
import spacy
import pandas as pd
import gc
import numpy as np
from json import loads, dumps
from config import *

# global variables
word_dict = {}
word_sequences = []
nlp = English()
tokenizer = Tokenizer(nlp.vocab)
nlp.vocab.add_flag(lambda s: s.lower() in spacy.lang.en.stop_words.STOP_WORDS, spacy.attrs.IS_STOP)
model = None


def infer(questions):
    print(questions)
    docs = nlp.pipe(questions, n_threads=2)
    seqs = []
    for doc in docs:
        seq = []
        for token in doc:
            idx = word_dict.get(token.text.lower(), -1)
            if idx != -1:
                seq.append(idx)
        seqs.append(seq)

    features = pad_sequences(seqs, maxlen=55, padding='post')
    pred_prob = np.squeeze(model.predict(features, batch_size=1), 1)
    return pred_prob.tolist()


def setup_producer():
    producer = KafkaProducer(
        bootstrap_servers=BOOTSTRAP_SERVERS,
        value_serializer=lambda x: dumps(x).encode('ascii')
    )
    return producer


def setup_kafka():
    consumer = KafkaConsumer(
        bootstrap_servers=BOOTSTRAP_SERVERS,
        auto_offset_reset='latest',
        enable_auto_commit=True,
        group_id='question-consumer-inference',
        value_deserializer=lambda x: loads(x.decode('utf-8'))
    )

    producer = setup_producer()
    consumer.subscribe([CONSUME_TOPIC])

    try:
        while True:
            records = consumer.poll(100, 50)
            record_list = []
            for tp, consumer_records in records.items():
                for consumer_record in consumer_records:
                    record_list.append(consumer_record.value)
                    # print(consumer_record.value)
                questions = [r.get('question') for r in record_list]
                q_idx = [r.get('id') for r in record_list]
                c_idx = [r.get('channelId') for r in record_list]
                result = infer(questions)
                # print(result)
                pairs = list(zip(q_idx, c_idx, result))
                # print(pairs)
                for pair in pairs:
                    out = {'id': pair[0], 'channelId': pair[1], 'isInsincereScore': pair[2]}
                    producer.send(PRODUCE_TOPIC, out)
    except KeyboardInterrupt:
        pass
    finally:
        consumer.close()


def building_word_dict():
    train = pd.read_csv('./resources/train.csv').fillna(' ')
    test = pd.read_csv('./resources/test.csv').fillna(' ')
    train_text = train['question_text']
    test_text = test['question_text']
    text_list = pd.concat([train_text, test_text])

    # Mapping a word to index
    print("Building word dictionary")

    word_index = 1
    docs = nlp.pipe(text_list, n_threads=3)
    for doc in docs:
        word_seq = []
        for token in doc:
            if (token.text not in word_dict) and (token.pos_ != "PUNCT"):
                word_dict[token.text.lower()] = word_index
                word_index += 1
            if token.pos_ != "PUNCT":
                word_seq.append(word_dict[token.text.lower()])
        word_sequences.append(word_seq)
    del docs
    gc.collect()


def setup_model():
    # workaround for gpu memory exceeding error
    gpus = tf.config.experimental.list_physical_devices('GPU')
    for gpu in gpus:
        tf.config.experimental.set_memory_growth(gpu, True)

    return keras.models.load_model("./resources/model.h5")


if __name__ == '__main__':
    building_word_dict()

    model = setup_model()

    setup_kafka()

    while True:
        pass  # never terminate
