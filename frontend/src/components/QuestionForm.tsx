import React from "react";
import { SERVER_URL } from "../Values";

interface Props {
  token: string,
}

interface State {
  questionContent: string,
  questionerName: string,
}

class QuestionForm extends React.Component<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = {
      questionContent: "",
      questionerName: "Annoymous",
    }
    this.handleSubmit = this.handleSubmit.bind(this);
  }

  handleSubmit(e: React.FormEvent<EventTarget>) {
    e.preventDefault()
    const url = `${SERVER_URL}/api.v1.qask/question/audience/create`

    const postBody = {
      token: this.props.token,
      content: this.state.questionContent,
      questionerName: this.state.questionerName
    }
    const requestMetadata = {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(postBody)
    }

    fetch(url, requestMetadata)
      .then(res =>
        console.log("posted")
      )
  }


  render() {
    return (
      <div className="flex flex-col py-3 bg-white upper-shadow px-2">
        <form onSubmit={e => this.handleSubmit(e)}>
          <textarea
            className="w-full form-input p-1 h-8 text-xs"
            placeholder="Type your question here"
            onChange={e => this.setState({ questionContent: e.target.value })}
            required
          />
          <div className="w-full flex flex-row-reverse">
            <button className="submit-btn" type="submit" value="Submit">
              Submit
            </button>
            <input className="form-input mr-5"
              type="text"
              placeholder="Your name"
              onChange={e => this.setState({ questionerName: e.target.value })}
            >
            </input>
          </div>

        </form>
      </div>
    );
  }
}

export default QuestionForm;
