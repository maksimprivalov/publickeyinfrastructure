import { Location, NavigateFunction } from "react-router-dom";

export const history = {
    navigate: (() => {}) as NavigateFunction,
    location: {} as Location
}