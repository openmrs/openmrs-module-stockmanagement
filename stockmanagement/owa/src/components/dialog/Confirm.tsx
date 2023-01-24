import React,{ReactNode} from "react";
import {ModalWrapper} from 'carbon-components-react'


export const ShowModal = (
    modalHeading: ReactNode = "Confirm",
    modalContent:ReactNode,
    handleSubmit: (args: any)=>boolean,
    callbackArgs: any,
    modalLabel?: ReactNode,
    shouldCloseAfterSubmit? : boolean )=>{

    return <>
    <ModalWrapper size="sm"   
      modalHeading={modalHeading}
      modalLabel={modalLabel}
      shouldCloseAfterSubmit={shouldCloseAfterSubmit}
      handleSubmit={() => handleSubmit(callbackArgs)}>
        {modalContent}      
    </ModalWrapper>
    </>
}