import React from 'react';
import  ReactDOM  from 'react-dom';
import { Provider } from 'react-redux';
import { store } from './app/store';
import App from './App';
import reportWebVitals from './reportWebVitals';
import { HashRouter as Router } from 'react-router-dom'
import { setupI18n } from "./locale";
import './index.scss';
import { integrateBreakpoints } from './core/utils/layoutUtils'

integrateBreakpoints();

setupI18n().catch((err) => console.error(`Failed to initialize translations`, err))

ReactDOM.render(
  <React.StrictMode>
    <Provider store={store}>
      <Router>               
        <App />
      </Router>
    </Provider>
  </React.StrictMode>,
    document.getElementById('stockmgmt_root')
);

// If you want to start measuring performance in your app, pass a function
// to log results (for example: reportWebVitals(console.log))
// or send to an analytics endpoint. Learn more: https://bit.ly/CRA-vitals
reportWebVitals();
