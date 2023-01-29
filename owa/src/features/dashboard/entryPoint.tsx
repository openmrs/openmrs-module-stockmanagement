import React, { useEffect } from "react"
import { Splash } from "../../components/spinner/Splash"
import { useGetDashboardExtensionsQuery } from '../../core/api/session'
import { htmlSafeId, toErrorMessage } from '../../core/utils/stringUtils'
import { BASE_URL_CONFIGURED, URL_STOCK_HOME } from '../../config'
import useTranslation from "../../core/utils/translation"
import { BreadCrumbs } from "../../core/utils/breadCrumbs"

export const EntryPoint = () => {
    const { data: dashboardExtensions, isFetching, isSuccess, error } = useGetDashboardExtensionsQuery();
    const { t } = useTranslation();
    useEffect(() => {
        new BreadCrumbs().withHome()
            .withLabel(t("stockmanagement.dashboard.title"), URL_STOCK_HOME)
            .generateBreadcrumbHtml();
    }, [t]);
    return <>
        <div id="stockmgmt-tasks" className="row">
            {isFetching &&
                <div className="force-splash-relative">
                    <Splash active={true} />
                </div>
            }
            {
                !isFetching && isSuccess && dashboardExtensions.results?.map((extension, index) => {
                    return <a key={`${extension.id}${index}`} id={htmlSafeId(extension.id)} href={`${BASE_URL_CONFIGURED}${extension.url}`} className="btn btn-default btn-lg button app big align-self-center" type="button">
                        {extension.icon && <i className={extension.icon}></i>}
                        {extension.label}
                    </a>
                })
            }
            {
                !isFetching && !isSuccess && error && <span className="error-text">{t('stockmanagement.dashboard.loaderror')} {toErrorMessage(error)}</span>
            }
        </div>
    </>
}

export default EntryPoint;