import './App.css';
import { BrowserRouter, Switch, Route } from 'react-router-dom';
import QuestionPage from './pages/QuestionPage';
import CreateChannelPage from './pages/CreateChannelPage';


function App() {
  return (
    <div className="App">
      <BrowserRouter>
        <Switch>
          <Route exact path="/" component={CreateChannelPage} />
          <Route exact path="/ask/:user/:token" component={QuestionPage} />
        </Switch>
      </BrowserRouter>
    </div>
  );
}

export default App;
