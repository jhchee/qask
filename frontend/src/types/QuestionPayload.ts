import Status from "./Status";

interface QuestionPayload {
    content: string,
    questionerName: string,
    likeCount: number,
    sent: string,
    isDeleted: boolean,
    isHidden: boolean,
    status: Status
    id: string,
}

export default QuestionPayload;