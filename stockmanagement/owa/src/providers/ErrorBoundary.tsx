import React, {ErrorInfo} from "react";
import { UnknownError } from '../components/error/UnknownError'
export class ErrorBoundary extends React.Component<{}, { hasError: boolean, error: Error | null }> {
  constructor(props: {}) {
      super(props);
      this.state = {hasError: false, error: null };
  }

  static getDerivedStateFromError(error: Error) {    // Update state so the next render will show the fallback UI.
      return {hasError: true, error: Error};
  }

  componentDidCatch(error: Error, errorInfo: ErrorInfo) {    // You can also log the error to an error reporting service
      console.log(error, errorInfo);
  }

  render() {
      if (this.state.hasError) {    // You can render any custom fallback UI
          return <UnknownError error={this.state.error}/>;
      }
      return this.props.children;
  }
}