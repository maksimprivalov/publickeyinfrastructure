import type { RouteObject } from "react-router-dom";
import { Test } from "./components/Test";
import AuthPage from './pages/AuthPage';

const paths: RouteObject[] = [
    {
        path: '/test',
        element: <Test />
    },
    {
        path: '/auth',
        element: <AuthPage />
    }
];

export default paths;
