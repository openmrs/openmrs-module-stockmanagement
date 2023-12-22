import React, { useEffect, useState } from 'react';
import { useLayoutType, isDesktopLayout } from '../../core/utils/layoutUtils';
import TrashCan from '@carbon/icons-react/lib/trash-can/16';
import {
    DataTable,
    Table,
    TableBody,
    TableCell,
    TableContainer,
    TableHead,
    TableHeader,
    TableRow,
    Button,
    ComboBox,
    TextInput,
    DataTableSkeleton,
} from 'carbon-components-react';
import styles from '../../root.module.scss';
import useTranslation from '../../core/utils/translation';
import { getStockOperationUniqueId, toErrorMessage } from '../../core/utils/stringUtils';
import produce from "immer";
import { Save24, Undo24 } from '@carbon/icons-react';
import {StockItemReferenceDTO} from '../../core/api/types/stockItem/StockItemReference';
import { StockSource } from '../../core/api/types/stockOperation/StockSource';
import {useLazyGetStockSourcesQuery} from "../../core/api/stockSource";
import { errorAlert } from '../../core/utils/alert';
import {StockItemPackagingUOMDTO} from "../../core/api/types/stockItem/StockItemPackagingUOM";



interface StockItemReferenceTableProps {
    items: StockItemReferenceDTO[];
    canEdit: boolean;
    setReferences: React.Dispatch<React.SetStateAction<StockItemReferenceDTO[]>>;
    actions: {
        onGoBack: () => void;
        onSave: () => void;
        onRemoveItem: (itemDto: StockItemReferenceDTO) => void;
    },
    setSelectedTab: React.Dispatch<React.SetStateAction<number>>;
    errors: { [key: string]: { [key: string]: boolean } };
    setItemValidity: React.Dispatch<React.SetStateAction<{ [key: string]: { [key: string]: boolean } }>>;
    validateItems: () => boolean;
}

const handleErrors = (payload: any) => {
    var errorMessage = toErrorMessage(payload);
    errorAlert(`${errorMessage}`);
    return;
  }

const StockOperationItemsTable: React.FC<StockItemReferenceTableProps> = ({
    items,
    canEdit,
    actions,
    setReferences,
    setSelectedTab,
    errors,
    setItemValidity,
    validateItems
}) => {
    console.log(items, "Get the Stock Item References")
    const { t } = useTranslation();
    const isDesktop = isDesktopLayout(useLayoutType());
    const [stockSourceReferences, setStockSourceReferences] = useState<StockSource[]>([]);
    const [getStockSource, { data: stockSource, isFetching: isFetchingStockSource }] = useLazyGetStockSourcesQuery()
    useEffect(() => {
        async function loadLookups() {
            if (canEdit) {
                if (!stockSource) {
                    // @ts-ignore
                    await getStockSource().unwrap()
                        .then((payload: any) => {
                            if ((payload as any).error) {
                                handleErrors(payload);
                                return;
                            }
                            let source = payload?.results as StockSource[];
                            setStockSourceReferences(source != null ? source:source);
                        })
                        .catch((error: any) => handleErrors(error));
                }
            }
        }
        loadLookups();

    }, [canEdit, stockSource, getStockSource]);

    const onReferenceChanged = (row: StockItemReferenceDTO, data: { selectedItem: any }) => {
        console.log(data, "This is Data")
        console.log(row, "This is Row")
        setReferences(
            produce((draft) => {
                const item = draft.find((p) => p.uuid === row.uuid);
                if (item) {
                    if (data.selectedItem) {
                        item.stockSourceName = data.selectedItem?.name;
                        item.stockSourceUuid = data.selectedItem?.uuid;
                        if (item.uuid === items[items.length - 1].uuid) {
                            let itemId = `new-item-${getStockOperationUniqueId()}`;
                            draft.push({ uuid: itemId, id: itemId } as StockItemReferenceDTO);
                        }
                    }
                    else {
                        item.stockSourceName = data.selectedItem?.name;
                        item.stockSourceUuid = data.selectedItem?.uuid;
                    }
                }
            })
        );

        setItemValidity(produce((draft) => {
            if (!(row.uuid! in draft)) draft[row.uuid!] = {};
            draft[row.uuid!]["stockSourceUuid"] = true;
        }));
    }

    const onReferenceCodeFieldChange = (row: StockItemReferenceDTO, value: string) => {
        try {
            let code: string | null = null;
            code = value;
            setReferences(
                produce((draft) => {
                    const item = draft.find((p) => p.uuid === row.uuid);
                    if (item) {
                        item.referenceCode = code;
                        if (item.uuid === draft[draft.length - 1].uuid) {
                            let itemId = `new-item-${getStockOperationUniqueId()}`;
                            draft.push({ uuid: itemId, id: itemId } as StockItemReferenceDTO);
                        }
                    }
                })
            );
        } catch (e) {
            console.log(e);
        }

        setItemValidity(produce((draft) => {
            if (!(row.uuid! in draft)) draft[row.uuid!] = {};
            draft[row.uuid!]["factor"] = true;
        }));
    }

    const headers = [
        { key: 'source', header: t('stockmanagement.stockitem.references.source'), styles: { width: "20%" } },
        { key: 'referenceCode', header: t('stockmanagement.stockitem.references.referencecode'), styles: { width: "10%" } }
    ];

    const onRemoveItem = (item: StockItemReferenceDTO, event: React.MouseEvent<HTMLButtonElement>) => {
        event.preventDefault();
        if (item.uuid?.startsWith("new-item")) {
            let itemId = item.uuid;
            if (itemId === items[items.length - 1].uuid) {
                return;
            }
            setReferences(
                produce((draft) => {
                    const itemIndex = draft.findIndex((p) => p.uuid === itemId);
                    if (itemIndex >= 0) {
                        draft.splice(itemIndex, 1);
                    }
                })
            );
        } else {
            actions.onRemoveItem(item);
        }
    }

    const onGoBack = (event: React.MouseEvent<HTMLAnchorElement>) => {
        event.preventDefault();
        actions.onGoBack();
    }

    const handleSave = async () => {
        try {
            actions.onSave();
        } finally {
        }
    }

    if(canEdit && (isFetchingStockSource || !stockSourceReferences || stockSourceReferences.length === 0)){
        return <DataTableSkeleton className={styles.dataTableSkeleton} showHeader={false} rowCount={5} columnCount={5} zebra />;
    }

    return <>
        <div className={`${styles.tableOverride} stkpg-operation-items`}>
            <DataTable rows={items as any} headers={headers} isSortable={false} useZebraStyles={true}
                render={({ rows, headers, getHeaderProps, getTableProps, getRowProps, getSelectionProps, getBatchActionProps, selectedRows }) => (
                    <TableContainer>
                        <Table {...getTableProps()}>
                            <TableHead>
                                <TableRow>
                                    {headers.map((header: any, index) => (
                                        <TableHeader
                                            {...getHeaderProps({
                                                header,
                                                isSortable: false,
                                            })}

                                            className={isDesktop ? styles.desktopHeader : styles.tabletHeader}
                                            style={header?.styles}
                                            key={`${header.key}`}>
                                            {header.header?.content ?? header.header}
                                        </TableHeader>
                                    ))}
                                    {canEdit && <TableHeader style={{ width: "70%" }}></TableHeader>}
                                </TableRow>
                            </TableHead>
                            <TableBody>
                                {items.map((row: any, rowIndex) => {
                                    return (
                                        <TableRow
                                            className={isDesktop ? styles.desktopRow : styles.tabletRow}
                                            key={row.uuid}>
                                            <TableCell>
                                                {
                                                    canEdit && row?.uuid?.startsWith('new-item') && <>
                                                        <ComboBox size='sm' titleText="" id={`item-${row.uuid}`}
                                                            initialSelectedItem={(row?.stockSourceUuid) ? {
                                                                uuid: row?.stockSourceUuid,
                                                                name: row?.stockSourceName
                                                            } as any : null}
                                                            selectedItem={row?.stockSourceUuid ? {
                                                                uuid: row?.stockSourceUuid,
                                                                name: row?.stockSourceName
                                                            } : null}
                                                            items={row?.references ? [...(stockSourceReferences.some(x => x.uuid === row?.stockSourceUuid) ? [] : [{ uuid: row?.stockSourceUuid, name: row?.stockSourceName }]), ...(stockSourceReferences ?? [])] : stockSourceReferences}
                                                            onChange={(data: { selectedItem: any }) => onReferenceChanged(row, data)}
                                                            shouldFilterItem={(data) => true}
                                                            itemToString={item => item?.name }
                                                            placeholder={'Filter...'}
                                                            invalid={(row.uuid in errors) && ("stockSourceUuid" in errors[row.uuid]) && !errors[row.uuid]["stockSourceUuid"]}
                                                        /></>
                                                }
                                                {(!canEdit || !row.uuid?.startsWith('new-item')) && row?.stockSourceName}
                                            </TableCell>
                                            <TableCell>
                                                {canEdit && <TextInput size='sm' id={`referencecode-${row.uuid}`} value={row?.referenceCode ?? ""} onChange={e=>onReferenceCodeFieldChange(row,e?.target?.value)} title="" invalidText="" labelText={""} invalid={(row.uuid in errors) && ("referenceCode" in errors[row.uuid]) && !errors[row.uuid]["referenceCode"]}/>}
                                                {!canEdit && row?.referenceCode?.toLocaleString()}
                                            </TableCell>
                                            {canEdit && <TableCell>
                                                <Button type="button" size="sm" className="submitButton clear-padding-margin" iconDescription={"Delete"} kind="ghost" renderIcon={TrashCan} onClick={(e) => onRemoveItem(row, e)} />
                                            </TableCell>}
                                        </TableRow>
                                    )
                                })}
                            </TableBody>
                        </Table>
                    </TableContainer>
                )}
            >
            </DataTable>
            <div className="table-bottom-border"></div>
        </div>
        {canEdit &&
            <div className='stkpg-form-buttons'>
                <Button name="save" type="submit" className="submitButton" onClick={handleSave} kind="primary" renderIcon={Save24}>{t("stockmanagement.save")}</Button>
                <Button type="button" className="cancelButton" kind="tertiary" onClick={onGoBack} renderIcon={Undo24}>{t("stockmanagement.goback")}</Button>
            </div>
        }
    </>;
};

export default StockOperationItemsTable;
