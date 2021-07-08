import React from "react";
import QuestionForm from "../components/QuestionForm";
import {
  RSocketClient,
  BufferEncoders,
  encodeAndAddWellKnownMetadata,
  MESSAGE_RSOCKET_COMPOSITE_METADATA,
  MESSAGE_RSOCKET_ROUTING
} from 'rsocket-core';
import RSocketWebSocketClient from 'rsocket-websocket-client';
import { RouteComponentProps } from 'react-router';
import QuestionBar from "../components/QuestionBar";
import Status from "../types/Status";
import User from "../types/User";
import QuestionPayload from "../types/QuestionPayload";
import { SERVER_URL } from "../Values";

interface Props extends RouteComponentProps<MatchParams> {
}

interface State {
  token: string,
  user: User,
  questions: QuestionPayload[],
}

interface MatchParams {
  user: string,
  token: string;
}


class QuestionPage extends React.Component<Props, State> {
  private _client: RSocketClient<string, Buffer> = new RSocketClient({
    transport: new RSocketWebSocketClient(
      {
        url: `ws://${SERVER_URL}/rsocket`,
      },
      BufferEncoders,
    ),
    setup: {
      dataMimeType: 'application/json',
      metadataMimeType: MESSAGE_RSOCKET_COMPOSITE_METADATA.string,
      keepAlive: 5000,
      lifetime: 60000,
    }
  })

  constructor(props: Props) {
    super(props);

    this.state = {
      token: this.props.match.params.token,
      user: this.props.match.params.user === User.AUDIENCE ? User.AUDIENCE : User.HOST,
      questions: [],
    }
    this.submitQuestion = this.submitQuestion.bind(this);
    this.connect = this.connect.bind(this);
  }

  componentDidMount() {
    this.connect()
  }

  submitQuestion(question: QuestionPayload) {
    const questions = this.state.questions

    const idx = questions.findIndex(q => q.id === question.id)
    if (idx === -1) {
      // does not exist
      questions.push(question)
    }
    else {
      // update the question if it already exists
      questions[idx] = question
    }

    this.setState({ questions: questions })
  }


  connect() {
    this._client.connect()
      .then(rsocket => {
        const token = this.state.token
        const endpoint = "api.v1.qask.stream/" + token;

        rsocket.requestStream({
          metadata: encodeAndAddWellKnownMetadata(
            Buffer.alloc(0),
            MESSAGE_RSOCKET_ROUTING,
            Buffer.from(String.fromCharCode(endpoint.length) + endpoint)
          )
        })
          .subscribe({
            onSubscribe: (s) => {
              console.log("subscribed")
              s.request(1000)
            },
            onNext: (e) => {
              if (e.data != null) {
                var question: QuestionPayload = JSON.parse(e.data);
                this.submitQuestion(question);
              }
            }
          });
      });
  }


  render() {
    return (
      <div>
        <div className="flex flex-col place-items-center space-y-4 bg-green-400 py-5">
          {this.state.questions.filter(question => question.status === Status.QUEUED && !question.isDeleted).map(({
            content,
            questionerName,
            likeCount,
            sent,
            status,
            isHidden,
            isDeleted,
            id,
          }) => (
            <QuestionBar
              key={id}
              content={content}
              questionerName={questionerName}
              likeCount={likeCount}
              sent={sent}
              id={id}
              token={this.state.token}
              user={this.state.user}
              isHidden={isHidden}
              isDeleted={isDeleted}
              status={status}
            />
          ))}
        </div>

        <div className="flex flex-col place-items-center space-y-4 mb-20 py-5">
          {this.state.questions.filter(question => question.status === Status.DEFAULT && !question.isDeleted).map(({
            content,
            questionerName,
            likeCount,
            sent,
            status,
            isHidden,
            isDeleted,
            id,
          }) => (
            <QuestionBar
              key={id}
              content={content}
              questionerName={questionerName}
              likeCount={likeCount}
              sent={sent}
              id={id}
              token={this.state.token}
              user={this.state.user}
              isHidden={isHidden}
              isDeleted={isDeleted}
              status={status}
            />
          ))}</div>

        <div className={"footer " + (this.state.user === User.HOST ? 'hidden' : 'visible')} >
          <QuestionForm token={this.state.token} />
        </div>
      </div>

    );
  }
}

export default QuestionPage;
