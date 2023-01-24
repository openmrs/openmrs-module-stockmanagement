import React from 'react';
import { AppRoutes } from './routes'
import { AppContextProvider } from './providers/AppContextProvider'
import './App.scss'
import { useNavigate } from 'react-router-dom';

function App() {
  let navigate = useNavigate();

  const interceptHrefNavigation = (e: any) => {
    navigate(e);
  }

  (window as any)["StockMgmtNavWithReactRouter"] = interceptHrefNavigation;
  
  return (
    <AppContextProvider>
      <AppRoutes />
    </AppContextProvider>
  )

}

export default App;
