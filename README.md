# Qask (Live Q&A app)
Qask is live Q&A app built using React and Spring Boot. 
This project highlights the use of reactive stream (with the help of [Rsocket](https://rsocket.io/)) to stream questions in real-time. 

Rsocket is designed to be an efficient counterpart of HTTP/2. It is a binary protocol that based on the reactive stream specification so that the server can maintain communications while not blocking other computations on the thread. You can read more of the benefits over [here](https://rsocket.io/about/motivations).

> ❗ Websocket is still used under the hood, make sure your browser supports it.

The entire project also fully leverages the non-blocking, Reactor API to deal with IO request in non-blocking way. It is deeply integrated with Kotlin coroutines for writing imperative (much more understandle) asynchronous code.

The webclient is written in Typesript, since the documentation on type annotations is really lacking, hopefully this can serve as good reference who wish to write the Rsocket client in Typescript. You can refer the type declaration in `QuestionPage.tsx`.

## Run the project
``` bash
cd frontend && yarn start

cd backend && ./gradlew bootRun
```

## Demo
