from kafka import KafkaConsumer, KafkaProducer
from json import loads, dumps
from config import *


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
                result = [0.40] * len(questions)
                # print(result)
                pairs = list(zip(q_idx, c_idx, result))
                print(pairs)
                for pair in pairs:
                    out = {'id': pair[0], 'channelId': pair[1], 'isInsincereScore': pair[2]}
                    producer.send(PRODUCE_TOPIC, out)
    except KeyboardInterrupt:
        pass
    finally:
        consumer.close()


if __name__ == '__main__':

    setup_kafka()

    while True:
        pass  # never terminate
