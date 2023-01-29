import React from "react"
import { Navigate, Route, Routes } from "react-router-dom";
import { URL_STOCK_REPORTS } from "../../config";
import { UserContextState } from "../../core/api/types/identity/UserContextState";
import RequirePriviledge from "../../routes/RequirePriviledge";
import ReportList from './ReportList';
import { APP_STOCKMANAGEMENT_REPORTS } from '../../core/privileges'

export interface ReportsProps {
    userContext: UserContextState
}

export const Locations: React.FC<ReportsProps> = ({
    userContext
}) => {
    return <div className="row">
        <div className="col-12">
            <Routes>
                <Route key="list-reports-route" path={"/"} element={<RequirePriviledge requireAllPrivileges={true} privileges={[APP_STOCKMANAGEMENT_REPORTS]} userContext={userContext}><ReportList /></RequirePriviledge>} />
                <Route key="default-reports-route" path={"*"} element={<Navigate to={URL_STOCK_REPORTS} />} />,
            </Routes>
        </div>
    </div>
}

export default Locations;
