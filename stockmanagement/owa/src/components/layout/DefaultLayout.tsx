import React, { useEffect, useState } from 'react';
import { Splash } from '../spinner/Splash';
import { selectStatus } from '../../features/auth/authSlice';
import LoadingStatus from '../../core/loadingStatus';
import { useAppSelector } from '../../app/hooks';

type DefaultLayoutProps = {
    children: React.ReactNode;
  };
  
export const DefaultLayout = ({ children }: DefaultLayoutProps) => {
    const loadingStatus = useAppSelector(selectStatus);
    const [active, setActive] = useState(true);
    useEffect(() => {
      setActive(loadingStatus === LoadingStatus.LOADING);
    }, [loadingStatus]);
    return <>
    <Splash active={active} />
    {children}
    </>
}