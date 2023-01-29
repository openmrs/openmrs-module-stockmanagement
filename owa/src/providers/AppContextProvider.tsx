import { AccordionSkeleton } from 'carbon-components-react';
import * as React from 'react';
import { ErrorBoundary } from './ErrorBoundary';

type AppContextProviderProps = {
    children: React.ReactNode;
};

export const AppContextProvider = ({ children }: AppContextProviderProps) => {
    return process.env.NODE_ENV === "development" ? (
        <React.Suspense fallback={<AccordionSkeleton count={3} />}>
            {children}
        </React.Suspense>
    ) : (
        <ErrorBoundary>
            <React.Suspense fallback={<AccordionSkeleton count={3} />}>
                {children}
            </React.Suspense>
        </ErrorBoundary>
    );
};
