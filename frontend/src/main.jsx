/** Mounts the selected original React UI using hash routing compatible with static S3 hosting. */
import React from 'react';
import ReactDOM from 'react-dom/client';
import {HashRouter} from 'react-router-dom';
import 'bootstrap/dist/css/bootstrap.min.css';
import 'bootstrap/dist/js/bootstrap.bundle.min.js';
import './custom.css';
import App from './App.jsx';
import {AuthProvider} from './auth.jsx';
ReactDOM.createRoot(document.getElementById('root')).render(<HashRouter><AuthProvider><App/></AuthProvider></HashRouter>);
