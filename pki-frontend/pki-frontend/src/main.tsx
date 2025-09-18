import { createRoot } from 'react-dom/client'
import './index.css'
import { BrowserRouter as Router, useRoutes } from 'react-router-dom'
import paths from './routes.tsx';

export function AppRoutes() {
  return useRoutes(paths);
}

createRoot(document.getElementById('root')!).render(
  <Router>
    <AppRoutes />
  </Router>
)
