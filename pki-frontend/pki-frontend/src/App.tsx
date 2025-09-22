import { useLocation, useNavigate } from 'react-router-dom'
import './App.css'
import { history } from './services/history'

function App() {

  history.navigate = useNavigate();
  history.location = useLocation();

  return (
    <>
    </>
  )
}

export default App
