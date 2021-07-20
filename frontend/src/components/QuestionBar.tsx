import React from "react";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { faAngleUp, faPlus, faCheck, faCross, faTimes, faExclamationCircle } from "@fortawesome/free-solid-svg-icons";
import Action from "../types/Action";
import User from "../types/User";
import Status from "../types/Status";
import { SERVER_URL_WITH_HTTP } from "../Values";
import ReactTooltip from 'react-tooltip';

interface Props {
  content: string,
  questionerName: string,
  likeCount: number,
  sent: string,
  id: string,
  token: string,
  isDeleted: boolean,
  isInsincere: boolean,
  status: Status,
  user: User,
}


interface States {
  voted: boolean
}


class QuestionBar extends React.Component<Props, States> {
  constructor(props: Props) {
    super(props);
    this.state = {
      voted: false
    }
    this.handleAction = this.handleAction.bind(this)
  }

  formatInstant(time: string): string {
    return new Date(Date.parse(time)).toLocaleString()
  }

  handleAction(e: React.FormEvent<EventTarget>, action: Action) {
    e.preventDefault()
    const url = `${SERVER_URL_WITH_HTTP}/api.v1.qask/question/${this.props.user}/edit`

    if (action === Action.VOTE_UP) {
      this.setState({ voted: true })
    }

    if (action === Action.VOTE_DOWN) {
      this.setState({ voted: false })
    }

    const postBody = {
      id: this.props.id,
      action: action,
      token: this.props.token,
    }

    const requestMetadata = {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(postBody)
    }

    fetch(url, requestMetadata)
      .then(res =>
        console.log("edited")
      )
  }

  render() {
    return (
      <div className="flex flex-col items-start lg:w-2/5 w-4/5 py-5 bg-white px-5">
        <div className="text-xs font-semibold">{this.props.questionerName}</div>
        <div className="text-xs">
          {this.formatInstant(this.props.sent)}
        </div>
        <p className="mt-2 text-xs">{this.props.content}</p>

        {this.props.user === User.HOST && this.props.isInsincere === true ?
          <button className="my-5 text-xs align-middle text-red-400" >
            <FontAwesomeIcon icon={faExclamationCircle} />
            <span className="pl-2">
              This is marked as spam by system, you can take further action by deleting it.
            </span>
          </button>
          : null
        }

        <div className="w-full flex flex-row-reverse gap-x-3">

          {this.props.status !== Status.QUEUED ?
            <span className={"border-2 border-blue-500 rounded-2xl px-2 " + (this.state.voted ? 'bg-blue-500 text-white' : 'bg-white text-blue-500')}>
              <button className="gap-x-5 text-xs align-middle" onClick={e => this.state.voted ? this.handleAction(e, Action.VOTE_DOWN) : this.handleAction(e, Action.VOTE_UP)}>
                <FontAwesomeIcon icon={faAngleUp} />
                <span className="pl-2">
                  {this.props.likeCount}
                </span>
              </button>
            </span>
            : null
          }

          {this.props.user === User.HOST && this.props.status !== Status.QUEUED ?
            <span className="border-2 border-blue-500 bg-white-500 text-blue-500 rounded-2xl px-2">
              <button className="gap-x-5 text-xs align-middle" onClick={e => this.handleAction(e, Action.QUEUE)}>
                <FontAwesomeIcon icon={faPlus} />
                <span className="pl-2">
                  Question
                </span>
              </button>
            </span>
            : null
          }

          {this.props.user === User.HOST && this.props.status === Status.QUEUED ?
            <span className="border-2 border-blue-500 bg-white-500 text-blue-500 rounded-2xl px-2">
              <button className="gap-x-5 text-xs align-middle" onClick={e => this.handleAction(e, Action.ANSWERED)}>
                <FontAwesomeIcon icon={faCheck} />
                <span className="pl-2">
                  Answered
                </span>
              </button>
            </span>
            : null
          }

          {this.props.user === User.HOST ?
            <span className="border-2 border-blue-500 bg-white-500 text-blue-500 rounded-2xl px-2">
              <button className="gap-x-5 text-xs align-middle" onClick={e => this.handleAction(e, Action.DELETE)}>
                <FontAwesomeIcon icon={faTimes} />
                <span className="pl-2">
                  Delete
                </span>
              </button>
            </span>
            : null
          }

        </div>

      </div>
    );
  }
}
export default QuestionBar;
