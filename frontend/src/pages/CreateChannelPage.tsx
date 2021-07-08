import React, { useState } from "react";
import {BASE_URL, SERVER_URL} from "../Values";
// import SERVER_URL from "../Values";

interface Props {

}

interface States {
    channelNameInput: string,
    channelDescriptionInput: string,
    channelDurationInput: number,

    presenterLink: string,
    audienceLink: string
}

interface ChannelDetail {
    name: string,
    description: string,
    endTime: string,
    presenterToken: string,
    audienceToken: string,
}


class CreateChannelPage extends React.Component<Props, States> {
    constructor(props: Props) {
        super(props);

        this.state = {
            channelNameInput: "",
            channelDescriptionInput: "",
            channelDurationInput: 0,

            presenterLink: "",
            audienceLink: ""
        }

        this.createChannel = this.createChannel.bind(this);
    }

    createChannel(event: React.FormEvent<EventTarget>) {
        event.preventDefault()
        const url = `${SERVER_URL}/api.v1.qask/channel/host/create`
        const postBody = {
            name: this.state.channelNameInput,
            durationInMinute: this.state.channelDurationInput,
            description: this.state.channelDescriptionInput
        }
        const requestMetadata = {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(postBody)
        }

        fetch(url, requestMetadata)
            .then(res => res.json())
            .then(
                (result: ChannelDetail) => {
                    const presenterLink = `${BASE_URL}/ask/host/${result.presenterToken}`
                    const audienceLink = `${BASE_URL}/ask/audience/${result.audienceToken}`
                    this.setState({ presenterLink: presenterLink })
                    this.setState({ audienceLink: audienceLink })
                }
            )
    }

    render() {
        return (
            <div className="flex flex-col w-screen h-screen">
                <form className="flex flex-col m-auto w-1/2 place-items-center space-y-10" onSubmit={this.createChannel}>
                    <input className="form-input" type="text" name="name" onChange={e => this.setState({ channelNameInput: e.target.value })} placeholder="Name" required />
                    <input className="form-input" type="text" name="description" onChange={e => this.setState({ channelDescriptionInput: e.target.value })} placeholder="Description" required />
                    <input className="form-input" type="number" name="duration-in-minutes" onChange={e => this.setState({ channelDurationInput: parseInt(e.target.value) })} placeholder="Duration in minutes" required />
                    <input className="submit-btn mt-10" type="submit" value="Submit" />
                    <div className={(this.state.presenterLink != "" ? 'visible' : 'hidden')}>
                        <div className="text-xs"><span>Presenter link:  </span>{this.state.presenterLink}</div>
                        <div className="text-xs"><span>Audience link:  </span>{this.state.audienceLink}</div>
                    </div>
                </form>
            </div>
        )
    }

}


export default CreateChannelPage