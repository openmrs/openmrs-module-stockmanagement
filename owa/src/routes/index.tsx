import React from 'react';
import { Routes } from 'react-router-dom';
import { useAppSelector } from '../app/hooks';
import { selectUserContext, selectStatus, selectIsAuthenticated } from '../features/auth/authSlice';
import { protectedRoutes } from './protected';
import { publicRoutes } from './public';
import { useEffect, useState } from 'react';
import LoadingStatus from '../core/loadingStatus';
import { useGetSessionQuery } from '../core/api/session'
import { Splash } from '../components/spinner/Splash';
import { useLazyGetStockManagementSessionQuery } from '../core/api/session'

export const AppRoutes = () => {
    const [displayProtectedRoutes, setDisplayProtectedRoutes] = useState<boolean | null>(null);
    const userContext = useAppSelector(selectUserContext);
    const pageLoadingStatus = useAppSelector(selectStatus);
    const isAuthenticated = useAppSelector(selectIsAuthenticated);
    const { isLoading } = useGetSessionQuery();
    const [getStockManagementSession] = useLazyGetStockManagementSessionQuery();

    useEffect(() => {
        async function loadPrivileges() {
            let showProtectedRoutes = null;
            if (pageLoadingStatus !== LoadingStatus.LOADING) {
                showProtectedRoutes = false;
                if (isAuthenticated) {
                    await getStockManagementSession(null, false);
                    showProtectedRoutes = true;
                }
            }
            setDisplayProtectedRoutes(showProtectedRoutes);
        }
        loadPrivileges();
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [pageLoadingStatus, isAuthenticated]);

    return <>
        {isLoading && <Splash active={true} />}
        {!isLoading &&
            (displayProtectedRoutes != null &&
                <Routes>
                    {displayProtectedRoutes ? protectedRoutes(userContext!) : publicRoutes}
                </Routes>
            )
        }

    </>
};
