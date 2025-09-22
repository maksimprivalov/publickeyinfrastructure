import { createRoot } from 'react-dom/client'
import './index.css'
import { BrowserRouter as Router, useRoutes } from 'react-router-dom'
import paths from './routes.tsx';
import * as Toast from '@radix-ui/react-toast';

export function AppRoutes() {
  return useRoutes(paths);
}

createRoot(document.getElementById('root')!).render(
  <Toast.Provider swipeDirection="right">
    <Toast.Viewport style={{ position: 'fixed', bottom: 32, right: 32, zIndex: 9999 }} />
    <Router>
      <AppRoutes />
    </Router>
  </Toast.Provider>

)
