import React, { useEffect } from 'react';
import { Splash } from '../../components/spinner/Splash';
import {useLogoutMutation} from '../../core/api/session'
import {  useNavigate } from 'react-router-dom'

export const  Logout = ()=>{
const [logout] = useLogoutMutation();
const navigate = useNavigate();

useEffect(()=>{
        logout(null);        
},[logout, navigate])

    return <Splash active={true}></Splash>
}