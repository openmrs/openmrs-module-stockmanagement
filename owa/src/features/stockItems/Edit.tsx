import { useCallback, useEffect, useMemo, useState } from 'react';
import { Splash } from '../../components/spinner/Splash';
import { useParams, useNavigate, useLocation, } from "react-router-dom";
import useTranslation from '../../core/utils/translation';
import { AccordionSkeleton, Tab, Tabs } from 'carbon-components-react';
import { StockItemDTO } from '../../core/api/types/stockItem/StockItem';
import { EditStockItem } from './EditStockItem';
import { StockItemPackagingUOMDTO } from '../../core/api/types/stockItem/StockItemPackagingUOM';
import { URL_STOCK_HOME, URL_STOCK_ITEM, URL_STOCK_ITEMS, URL_STOCK_ITEMS_NEW, URL_STOCK_ITEMS_REDIRECT } from '../../config';
import { errorAlert, successAlert } from '../../core/utils/alert';
import { cloneDeep } from 'lodash-es';
import { useUrlQueryParams } from '../../core/utils/urlUtils';
import { useDeleteStockItemPackagingUnitMutation,useDeleteStockItemReferenceMutation,useCreateStockItemReferenceMutation, useLazyGetStockItemQuery, useCreateStockItemMutation, useUpdateStockItemMutation, useCreateStockItemPackagingUnitMutation, useUpdateStockItemPackagingUnitMutation, useDeleteStockRuleMutation, useUpdateStockItemReferenceMutation } from '../../core/api/stockItem';
import { TASK_STOCKMANAGEMENT_STOCKITEMS_MUTATE } from '../../core/privileges';
import { toErrorMessage } from '../../core/utils/stringUtils';
import { useHasPreviledge } from '../auth/AccessControl';
import { ConfirmModalPopup } from '../../components/dialog/ConfirmModalPopup';
import { BreadCrumbs } from '../../core/utils/breadCrumbs';
import StockItemTransactionsTable from './StockItemTransactions';
import { useLazyGetPartiesQuery } from '../../core/api/lookups';
import { APP_STOCKMANAGEMENT_STOCKITEMS } from '../../core/privileges';
import { useGetPrivilegeScopes } from '../auth/AccessControl';
import { DISPENSARY_LOCATION_TAG, MAIN_PHARMACY_LOCATION_TAG, MAIN_STORE_LOCATION_TAG } from '../../core/consts';
import StockItemBatchInformationTable from './StockItemBatchInformation';
import StockItemQuantitiesTable from './StockItemQuantities';
import StockItemPackagingUnitsTable from './StockItemPackagingUnitsTable';
import produce from 'immer';
import StockRulesTable from './StockRulesTable';
import { StockRule } from '../../core/api/types/stockItem/StockRule';
import StockItemReferenceTable from "./StockItemReferenceTable";
import {StockItemReferenceDTO} from "../../core/api/types/stockItem/StockItemReference";

const handleErrors = (payload: any) => {
  var errorMessage = toErrorMessage(payload);
  errorAlert(`${errorMessage}`);
  return;
}

export const Edit = () => {

  const { id } = useParams();
  const urlQueryParams = useUrlQueryParams();
  const currentLocation = useLocation();
  const { t } = useTranslation();
  const [showLoading, setShowLoading] = useState(true);
  const [showSplash, setShowSplash] = useState(false);
  const [isNew, setIsNew] = useState(false);
  const [editableModel, setEditableMode] = useState<StockItemDTO>({} as unknown as StockItemDTO);
  const [canEdit, setCanEdit] = useState(false);
  const [selectedTab, setSelectedTab] = useState(0);
  const [packagingUnits, setPackagingUnits] = useState<StockItemPackagingUOMDTO[]>([]);
  const [references, setReferences] = useState<StockItemReferenceDTO[]>([]);
  const navigate = useNavigate();
  const [getStockItem] = useLazyGetStockItemQuery();
  const [canUpdateStockItems] = useHasPreviledge([TASK_STOCKMANAGEMENT_STOCKITEMS_MUTATE], true);
  const [createStockItem] = useCreateStockItemMutation();
  const [updateStockItem] = useUpdateStockItemMutation();
  const [showConfirm, setShowConfirm] = useState(false);
  const [confirmHeading, setConfirmHeading] = useState("");
  const [confirmLabel, setConfirmLabel] = useState("");
  const [confirmText, setConfirmText] = useState("");
  const [confirmType, setConfirmType] = useState("");
  const [confirmDefaultReason, setConfirmDefaultReason] = useState("");
  const [confirmParam, setConfirmParam] = useState("");
  const [confirmRequireReason, setConfirmRequireReason] = useState(false);
  const [displayTransactions, setDisplayTransactions] = useState(false);
  const [displayPackagingUnits, setDisplayPackagingUnits] = useState(false);
  const [displayReferences, setDisplayReferences] = useState(false);
  const [displayBatchInformation, setDisplayBatchInformation] = useState(false);
  const [displayQuantities, setDisplayQuantities] = useState(false);
  const [displayStockRules, setDisplayStockRules] = useState(false);
  const userPrivilegeScopes = useGetPrivilegeScopes([APP_STOCKMANAGEMENT_STOCKITEMS, TASK_STOCKMANAGEMENT_STOCKITEMS_MUTATE]);
  const [getPartyList, { data: partyList }] = useLazyGetPartiesQuery();
  const [itemValidity, setItemValidity] = useState<{ [key: string]: { [key: string]: boolean } }>({});
  const [ruleValidity, setRuleValidity] = useState<{ [key: string]: { [key: string]: boolean } }>({});
  const [editablePackagingUnits, setEditablePackagingUnits] = useState<StockItemPackagingUOMDTO[]>([{ uuid: `new-item-1`, id: `new-item-1` }])
  const [editableReferences, setEditableReferences] = useState<StockItemReferenceDTO[]>([{ uuid: `new-item-1`, id: `new-item-1` }])
  const [createStockItemPackagingUnit] = useCreateStockItemPackagingUnitMutation();
  const [updateStockItemPackagingUnit] = useUpdateStockItemPackagingUnitMutation();
  const [deleteStockItemPackagingUnit] = useDeleteStockItemPackagingUnitMutation();
  const [deleteStockItemReference] = useDeleteStockItemReferenceMutation();
  const [createStockItemReference] = useCreateStockItemReferenceMutation();
  const [updateStockItemReference] = useUpdateStockItemReferenceMutation();
  const [deleteStockRule] = useDeleteStockRuleMutation();

  useEffect(() => {
    let breadCrumbs = new BreadCrumbs().withHome()
      .withLabel(t("stockmanagement.dashboard.title"), URL_STOCK_HOME)
      .withLabel(t("stockmanagement.stockitem.list.title"), URL_STOCK_ITEMS);
    if (isNew) {
      breadCrumbs.withLabel(t("stockmanagement.stockitem.new.title"), URL_STOCK_ITEMS_NEW);
    } else {
      breadCrumbs.withLabel((editableModel?.drugName ? `${editableModel?.drugName}${editableModel?.conceptName ? (` (${editableModel?.conceptName})`) : ""}` : null) ?? editableModel?.conceptName ?? '', URL_STOCK_ITEM(id!))
    }
    breadCrumbs.generateBreadcrumbHtml();
  }, [t, id, isNew, editableModel?.drugName, editableModel?.conceptName]);

  const partyLookupList = useMemo(() => {
    if (!userPrivilegeScopes || !partyList) {
      return [];
    }
    let locationUuids = userPrivilegeScopes?.filter(p => p.privilege === APP_STOCKMANAGEMENT_STOCKITEMS).map(p => p.locationUuid);
    return partyList.results.filter(p => locationUuids.includes(p.locationUuid) && p.tags &&
      p.tags.some(x => x === MAIN_STORE_LOCATION_TAG || x === MAIN_PHARMACY_LOCATION_TAG || x === DISPENSARY_LOCATION_TAG));
  }, [userPrivilegeScopes, partyList])

  const partyLookupUpdateableList = useMemo(() => {
    if (!userPrivilegeScopes || !partyList) {
      return [];
    }
    let locationUuids = userPrivilegeScopes?.filter(p => p.privilege === TASK_STOCKMANAGEMENT_STOCKITEMS_MUTATE).map(p => p.locationUuid);
    return partyList.results.filter(p => locationUuids.includes(p.locationUuid) && p.tags &&
      p.tags.some(x => x === MAIN_STORE_LOCATION_TAG || x === MAIN_PHARMACY_LOCATION_TAG || x === DISPENSARY_LOCATION_TAG));
  }, [userPrivilegeScopes, partyList])


  useEffect(() => {
    async function loadStockItem() {
      setShowLoading(true);
      let isNewStockItem = null === id || id === undefined || id.indexOf('-') < 0;
      setIsNew(isNewStockItem);
      let stockItem: StockItemDTO = {} as unknown as StockItemDTO;
      if (!isNewStockItem) {
        let failedToLoadStockItem = false;
        await getStockItem(id!, false).unwrap()
          .then((payload: any) => {
            if ((payload as any).error) {
              handleErrors(payload);
              failedToLoadStockItem = true;
              return;
            }
            stockItem = cloneDeep(payload) as StockItemDTO;
            stockItem["isDrug"] = stockItem.drugUuid != null;
            if (stockItem && stockItem.packagingUnits) {
              stockItem.packagingUnits.forEach((si, ii, iar) => {
                (si as any)["id"] = si.uuid;
              });
            }

            if (stockItem && stockItem.references) {
              stockItem.references.forEach((si, ii, iar) => {
                (si as any)["id"] = si.uuid;
              });
            }

          })
          .catch((error: any) => {
            handleErrors(error);
            failedToLoadStockItem = true;
            return;
          });
        if (failedToLoadStockItem) {
          navigate(URL_STOCK_ITEMS);
          return;
        }

        await getPartyList().unwrap()
          .then((payload: any) => {
            if ((payload as any).error) {
              handleErrors(payload);
              return;
            }

          })
          .catch((error: any) => handleErrors(error));
      }


      let canEditModel: boolean = false;
      let canViewModel: boolean = false;
      if (isNewStockItem) {
        stockItem = {} as any as StockItemDTO;
        canEditModel = canUpdateStockItems ?? false;
        setCanEdit(canEditModel);
        if (!canEditModel) {
          errorAlert(t("stockmanagement.stockitem.nomutateprivilegescopes"));
          navigate(URL_STOCK_ITEMS);
          return;
        }
      } else {
        canEditModel = stockItem?.permission?.canEdit ?? false;
        canViewModel = stockItem?.permission?.canView ?? false;
        setCanEdit(canEditModel);
      }

      if (!canEditModel && !canViewModel) {
        errorAlert(t("stockmanagement.stockitem.noaccessprivilegescopes"));
        navigate(URL_STOCK_ITEMS);
        return;
      }
      else {
        if (stockItem.packagingUnits) {
          setPackagingUnits(stockItem.packagingUnits);
          let editableUnits = cloneDeep(stockItem.packagingUnits);
          editableUnits.push({ uuid: `new-item-1`, id: `new-item-1` })
          setEditablePackagingUnits(editableUnits);
        }

        if (stockItem.references) {
          setReferences(stockItem.references);
          let editableReferences = cloneDeep(stockItem.references);
          editableReferences.push({ uuid: `new-item-1`, id: `new-item-1` })
          setEditableReferences(editableReferences);
        }

        try {
          let tabString: string | null = urlQueryParams.get("tab");
          if (tabString) {
            let tab: number = parseInt(tabString);
            if (tab >= 0 && tab <= 6) {
              setSelectedTab(tab);
              switch (tab) {
                case 1:
                  onClickTab("packagingunits");
                  break;
                case 2:
                  onClickTab("transactions");
                  break;
                case 3:
                  onClickTab("batch");
                  break;
                case 4:
                  onClickTab("quantities");
                  break;
                case 5:
                  onClickTab("stockrules");
                  break;
                case 6:
                  onClickTab("stockreferences");
                  break;
              }
            }
          }
        } catch (e) { }
      }
      setEditableMode(stockItem);
      setShowLoading(false);
    }
    loadStockItem();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id, currentLocation]);


  const getUpdateStockItemModel = useCallback((): any => {
    let stockItem = {
      hasExpiration: editableModel.hasExpiration,
      preferredVendorUuid: editableModel.preferredVendorUuid,
      dispensingUnitUuid: editableModel.dispensingUnitUuid,
      categoryUuid: editableModel.categoryUuid,
      commonName: editableModel.commonName,
      acronym: editableModel.acronym,
      expiryNotice: editableModel.hasExpiration ? editableModel.expiryNotice : null
    } as any;

    if (isNew) {
      stockItem = Object.assign(stockItem, {
        drugUuid: editableModel.drugUuid,
        conceptUuid: editableModel.conceptUuid
      });
    } else {
      stockItem = Object.assign(stockItem, {
        purchasePrice: editableModel.purchasePrice,
        purchasePriceUoMUuid: editableModel.purchasePriceUoMUuid,
        dispensingUnitPackagingUoMUuid: editableModel.dispensingUnitPackagingUoMUuid,
        defaultStockOperationsUoMUuid: editableModel.defaultStockOperationsUoMUuid,
        reorderLevel: editableModel.reorderLevel,
        reorderLevelUoMUuid: editableModel.reorderLevelUoMUuid
      });
    }

    return stockItem;
  }, [editableModel, isNew]);


  const handleSaveStockItem = useCallback(async () => {
    setShowSplash(true);
    let hideSplash = true;
    try {
      let stockItem = getUpdateStockItemModel();
      await (isNew ? createStockItem(stockItem) : updateStockItem({ model: stockItem, uuid: editableModel.uuid! })).unwrap().then(
        (payload: any) => {
          if ((payload as any).error) {
            var errorToken = toErrorMessage(payload);
            errorAlert(`${t(editableModel.uuid == null ? "stockmanagement.stockitem.createfailed" : "stockmanagement.stockitem.updatefailed")} ${errorToken}`);
            return;
          } else {

            setShowSplash(false);
            hideSplash = false;
            successAlert(`${t(editableModel.uuid == null ? "stockmanagement.stockitem.createsuccess" : "stockmanagement.stockitem.updatesuccess")}`);
            if (isNew) {
              navigate(URL_STOCK_ITEM((payload as StockItemDTO).uuid!));
            } else {
              navigate(URL_STOCK_ITEMS_REDIRECT((payload as StockItemDTO).uuid!, selectedTab?.toString() ?? ""));
            }
          }
        })
        .catch((error) => {
          var errorToken = toErrorMessage(error);
          errorAlert(`${t(editableModel.uuid == null ? "stockmanagement.stockitem.createfailed" : "stockmanagement.stockitem.updatefailed")} ${errorToken}`);
          return;
        });
    } finally {
      if (hideSplash) {
        setShowSplash(false);
      }
    }

  }, [getUpdateStockItemModel, isNew, createStockItem, updateStockItem, editableModel.uuid, t, navigate, selectedTab]);

  const onConfirmRemovePackagingUnit = async (itemUuid: string | undefined | null, reason: string | null): Promise<void> => {
    if (!itemUuid) {
      return;
    }
    setShowSplash(true);
    try {
      await deleteStockItemPackagingUnit(itemUuid).unwrap().then(
        (payload: any) => {
          if (payload && (payload as any).error) {
            var errorToken = toErrorMessage(payload);
            errorAlert(`${t("stockmanagement.stockitem.deletepackagingunitfailed")} ${errorToken}`);
            return;
          } else {
            successAlert(`${t("stockmanagement.stockitem.deletepackagingunitsuccess")}`);
            setEditablePackagingUnits(
              produce((draft) => {
                const itemIndex = draft.findIndex((p) => p.uuid === itemUuid);
                if (itemIndex >= 0) {
                  draft.splice(itemIndex, 1);
                }
              })
            );
          }
        })
        .catch((error) => {
          var errorToken = toErrorMessage(error);
          errorAlert(`${t("stockmanagement.stockitem.deletepackagingunitfailed")} ${errorToken}`);
          return;
        });
    } finally {
      setShowSplash(false);
    }
  }

  const onConfirmRemoveReference = async (itemUuid: string | undefined | null, reason: string | null): Promise<void> => {
    if (!itemUuid) {
      return;
    }
    setShowSplash(true);
    try {
      await deleteStockItemReference(itemUuid).unwrap().then(
          (payload: any) => {
            if (payload && (payload as any).error) {
              var errorToken = toErrorMessage(payload);
              errorAlert(`${t("stockmanagement.stockitem.deletereferencesfailed")} ${errorToken}`);
              return;
            } else {
              successAlert(`${t("stockmanagement.stockitem.deletereferencesuccess")}`);
              setEditableReferences(
                  produce((draft) => {
                    const itemIndex = draft.findIndex((p) => p.uuid === itemUuid);
                    if (itemIndex >= 0) {
                      draft.splice(itemIndex, 1);
                    }
                  })
              );
            }
          }).catch((error:any) => {
            var errorToken = toErrorMessage(error);
            errorAlert(`${t("stockmanagement.stockitem.deletereferencesfailed")} ${errorToken}`);
            return;
          });
    } finally {
      setShowSplash(false);
    }
  }

  const onConfirmRemoveStockRule = async (itemUuid: string | undefined | null, reason: string | null): Promise<void> => {
    if (!itemUuid) {
      return;
    }
    setShowSplash(true);
    try {
      await deleteStockRule(itemUuid).unwrap().then(
        (payload: any) => {
          if (payload && (payload as any).error) {
            var errorToken = toErrorMessage(payload);
            errorAlert(`${t("stockmanagement.stockitem.deletestockrulefailed")} ${errorToken}`);
            return;
          } else {
            successAlert(`${t("stockmanagement.stockitem.deletestockrulesuccess")}`);
          }
        })
        .catch((error) => {
          var errorToken = toErrorMessage(error);
          errorAlert(`${t("stockmanagement.stockitem.deletestockrulefailed")} ${errorToken}`);
          return;
        });
    } finally {
      setShowSplash(false);
    }
  }

  const onConfirmAction = (confirmType: string, reason: string | null, confirmParam?: string) => {
    setShowConfirm(false);
    switch (confirmType) {
      case "back":
        navigate(URL_STOCK_ITEMS);
        break;
      case "removepkgunit":
        onConfirmRemovePackagingUnit(confirmParam, reason);
        break;
      case "removestockrule":
        onConfirmRemoveStockRule(confirmParam, reason);
        break;
      case "removereference":
        onConfirmRemoveReference(confirmParam, reason);
        break;
    }
  }

  const onConfirmClose = () => {
    setConfirmDefaultReason("");
    setShowConfirm(false);
  }


  const handleGoBack = useCallback(() => {
    setConfirmText("stockmanagement.stockitem.confirm.back.text");
    setConfirmHeading("stockmanagement.stockitem.confirm.back.heading");
    setConfirmLabel("stockmanagement.stockitem.confirm.back.label");
    setConfirmType("back");
    setConfirmDefaultReason("");
    setConfirmRequireReason(false);
    setConfirmParam("");
    setShowConfirm(true);
  }, []);

  const onClickTab = (tab: string) => {
    switch (tab) {
      case "packagingunits":
        if (!isNew && !displayPackagingUnits) {
          setDisplayPackagingUnits(true);
        }
        break;
      case "transactions":
        if (!isNew && !displayTransactions) {
          setDisplayTransactions(true);
        }
        break;
      case "batch":
        if (!isNew && !displayBatchInformation) {
          setDisplayBatchInformation(true);
        }
        break;
      case "quantities":
        if (!isNew && !displayQuantities) {
          setDisplayQuantities(true);
        }
        break;
      case "stockrules":
        if (!isNew && !displayStockRules) {
          setDisplayStockRules(true);
        }
        break;
      case "stockreference":
        if (!isNew && !displayReferences) {
          setDisplayReferences(true);
        }
        break;
    }
  }

  const isPackagingUnitEmpty = useCallback((itemDto: StockItemPackagingUOMDTO): boolean => {
    if (itemDto == null) return true;
    if (itemDto.packagingUomUuid) return false;
    if (itemDto.factor) return false;
    return true;
  }, []);

  const isReferenceEmpty = useCallback((itemDto: StockItemReferenceDTO): boolean => {
    if (itemDto == null) return true;
    if (itemDto.stockSourceUuid) return false;
    if (itemDto.referenceCode) return false;
    return true;
  }, []);

  const validatePackagingUnits = useCallback(() => {
    let validItems = true;
    let validationStatus: { [key: string]: { [key: string]: boolean } } = {};
    let emptyCount = 0;
    editablePackagingUnits.forEach(p => {
      validationStatus[p.uuid!] = {};
      if (isPackagingUnitEmpty(p)) {
        emptyCount++;
        return;
      }

      if (!p.packagingUomUuid) {
        validationStatus[p.uuid!]["packagingUomUuid"] = false;
        validItems = false;
      }

      if (!p.factor || p.factor < 0) {
        validationStatus[p.uuid!]["factor"] = false;
        validItems = false;
      }
    });
    if (emptyCount === editablePackagingUnits.length) {
      validItems = false;
      errorAlert(t("stockmanagement.stockitem.units.oneitemrequired"));
    } else if (!validItems) {
      errorAlert(t("stockmanagement.stockitem.units.itemsnotvalid"));
    }
    setItemValidity(validationStatus);
    return validItems;
  }, [editablePackagingUnits, isPackagingUnitEmpty, t])


  const validateReferences = useCallback(() => {
    let validItems = true;
    let validationStatus: { [key: string]: { [key: string]: boolean } } = {};
    let emptyCount = 0;
    editableReferences.forEach(p => {
      validationStatus[p.uuid!] = {};
      if (isReferenceEmpty(p)) {
        emptyCount++;
        return;
      }

      if (!p.stockSourceUuid) {
        validationStatus[p.uuid!]["stockSourceUuid"] = false;
        validItems = false;
      }

      if (!p.referenceCode && p.referenceCode!=="") {
        validationStatus[p.uuid!]["referenceCode"] = false;
        validItems = false;
      }
    });
    if (emptyCount === editableReferences.length) {
      validItems = false;
      errorAlert(t("stockmanagement.stockitem.source.oneitemrequired"));
    } else if (!validItems) {
      errorAlert(t("stockmanagement.stockitem.source.itemsnotvalid"));
    }
    setItemValidity(validationStatus);
    return validItems;
  }, [editableReferences, isReferenceEmpty, t])

  const handleSavePackagingUnit = useCallback(async () => {
    setShowSplash(true);
    if (!validatePackagingUnits()) {
      setShowSplash(false);
      return;
    }
    let hideSplash = true;
    try {

      let items: any[] = []
      editablePackagingUnits.forEach(p => {
        if (p.uuid?.startsWith("new-item") && isPackagingUnitEmpty(p)) return;
        let newItem = {
          stockItemUuid: editableModel?.uuid,
          packagingUomUuid: p.packagingUomUuid,
          factor: p.factor
        } as any;
        if (!p.uuid?.startsWith("new-item")) {
          newItem["uuid"] = p.uuid;
        }
        items.push(newItem)
      });

      items.forEach(async uom => {
        await (!uom.uuid ? createStockItemPackagingUnit(uom) : updateStockItemPackagingUnit({ model: uom, uuid: uom.uuid! })).unwrap().then(
          (payload: any) => {
            if ((payload as any).error) {
              var errorToken = toErrorMessage(payload);
              errorAlert(`${t(editableModel.uuid == null ? "stockmanagement.stockitem.packagingunitcreatefailed" : "stockmanagement.stockitem.packagingunitupdatefailed")} ${errorToken}`);
              navigate(URL_STOCK_ITEMS_REDIRECT((editableModel as StockItemDTO).uuid!, selectedTab?.toString() ?? ""));
              return;
            } else {
              successAlert(`${t(editableModel.uuid == null ? "stockmanagement.stockitem.packagingunitcreatesuccess" : "stockmanagement.stockitem.packagingunitupdatesuccess")}`);
            }
          })
          .catch((error) => {
            var errorToken = toErrorMessage(error);
            errorAlert(`${t(editableModel.uuid == null ? "stockmanagement.stockitem.packagingunitcreatefailed" : "stockmanagement.stockitem.packagingunitupdatefailed")} ${errorToken}`);
            return;
          });
      });
      setShowSplash(false);
      hideSplash = false;
    } finally {
      if (hideSplash) {
        setShowSplash(false);
      }
    }

  }, [validatePackagingUnits, editablePackagingUnits, isPackagingUnitEmpty, editableModel, createStockItemPackagingUnit, updateStockItemPackagingUnit, t, navigate, selectedTab]);


  const handleRemovePackagingUnit = useCallback((itemDto: StockItemPackagingUOMDTO) => {
    let text = t("stockmanagement.stockitem.confirm.removepackagingunit.text");
    text = text.replaceAll("%item.name%", itemDto?.packagingUomName ?? "");
    setConfirmText(text);
    setConfirmHeading("stockmanagement.stockitem.confirm.removepackagingunit.heading");
    setConfirmLabel(itemDto?.packagingUomName!);
    setConfirmType("removepkgunit");
    setConfirmDefaultReason("");
    setConfirmRequireReason(true);
    setConfirmParam(itemDto.uuid ?? "");
    setShowConfirm(true);
  }, [t]);

  const handleSaveReference = useCallback(async () => {
    setShowSplash(true);
    if (!validateReferences()) {
      setShowSplash(false);
      return;
    }
    let hideSplash = true;
    try {

      let items: StockItemReferenceDTO[] = []
      editableReferences.forEach(p => {
        if (p.uuid?.startsWith("new-item") && isReferenceEmpty(p)) return;
        let newItem = {
          stockItemUuid: editableModel?.uuid,
          stockSourceUuid: p.stockSourceUuid,
          referenceCode: p.referenceCode
        } as StockItemReferenceDTO;
        if (!p.uuid?.startsWith("new-item")) {
          newItem["uuid"] = p.uuid;
        }
        items.push(newItem)
      });
      for (const stockItemReference of items) {
        await (!stockItemReference.uuid ? createStockItemReference(stockItemReference) : updateStockItemReference({ model: stockItemReference, uuid: stockItemReference.uuid! })).unwrap().then(
            (payload: any) => {
              if ((payload as any).error) {
                var errorToken = toErrorMessage(payload);
                errorAlert(`${t(editableModel.uuid == null ? "stockmanagement.stockitem.referencecreatefailed" : "stockmanagement.stockitem.referenceupdatefailed")} ${errorToken}`);
                navigate(URL_STOCK_ITEMS_REDIRECT((editableModel as StockItemDTO).uuid!, selectedTab?.toString() ?? ""));
                return;
              } else {
                successAlert(`${t(editableModel.uuid == null ? "stockmanagement.stockitem.referencecreatesuccess" : "stockmanagement.stockitem.referenceupdatesuccess")}`);
              }
            })
            .catch((error:any) => {
              var errorToken = toErrorMessage(error);
              errorAlert(`${t(editableModel.uuid == null ? "stockmanagement.stockitem.referencecreatefailed" : "stockmanagement.stockitem.referenceupdatefailed")} ${errorToken}`);
              return;
            });
      }
      setShowSplash(false);
      hideSplash = false;
    } finally {
      if (hideSplash) {
        setShowSplash(false);
      }
    }

  }, [validateReferences, editableReferences, isReferenceEmpty, editableModel, createStockItemReference, updateStockItemReference, t, navigate, selectedTab]);

  const handleRemoveReference = useCallback((itemDto: StockItemReferenceDTO) => {
    let text = t("stockmanagement.stockitem.confirm.removereference.text");
    text = text.replaceAll("%item.name%", itemDto?.stockSourceName ?? "");
    setConfirmText(text);
    setConfirmHeading("stockmanagement.stockitem.confirm.removereference.heading");
    setConfirmLabel(itemDto?.stockSourceName!);
    setConfirmType("removereference");
    setConfirmDefaultReason("");
    setConfirmRequireReason(true);
    setConfirmParam(itemDto.uuid ?? "");
    setShowConfirm(true);
  }, [t]);

  const handleRemoveStockRule = useCallback((itemDto: StockRule) => {
    let text = t("stockmanagement.stockitem.confirm.removestockrule.text");
    text = text.replaceAll("%item.name%", itemDto?.name ?? "");
    setConfirmText(text);
    setConfirmHeading("stockmanagement.stockitem.confirm.removestockrule.heading");
    setConfirmLabel(itemDto?.name!);
    setConfirmType("removestockrule");
    setConfirmDefaultReason("");
    setConfirmRequireReason(true);
    setConfirmParam(itemDto.uuid ?? "");
    setShowConfirm(true);
  }, [t]);

  if (showLoading) {
    return <AccordionSkeleton open count={4} />
  }

  return <>
    <Splash active={showSplash} blockUi={true} />
    {showConfirm && <ConfirmModalPopup showModal={showConfirm}
      confirmHeading={confirmHeading}
      confirmLabel={confirmLabel}
      confirmText={confirmText}
      confirmType={confirmType}
      onConfirm={onConfirmAction}
      onClose={onConfirmClose}
      defaultReason={confirmDefaultReason}
      requireReason={confirmRequireReason}
      confirmParam={confirmParam}
    />}
    <div className="stkpg-page skpg-stock-item">
      {!isNew &&
        <div className="stkpg-page-header clear-top-padding clear-bottom-padding">
          <h3 className="stkpg-page-title"><strong>{t(editableModel?.drugUuid ? "stockmanagement.drug" : "stockmanagement.item")}: </strong>{editableModel?.drugUuid ? (`${editableModel?.drugName}${editableModel?.conceptName ? (` (${editableModel?.conceptName})`) : ""}`) : editableModel?.conceptName}</h3>
        </div>
      }
      <div className="stkpg-page-body stkpg-clear-overflow skpg-tab-panel-no-horiz-padding">
        <Tabs selected={selectedTab} type="container" onSelectionChange={(e) => setSelectedTab(e)}>
          <Tab label="Stock Item Details">
            <EditStockItem model={editableModel!}
              setModel={setEditableMode}
              isNew={isNew}
              packagingUnits={packagingUnits}
              references={references}
              setShowSplash={setShowSplash}
              setSelectedTab={setSelectedTab}
              canEdit={canEdit} actions={{
                onGoBack: handleGoBack,
                onSave: handleSaveStockItem
              }} />
          </Tab>
          <Tab label="Packaging Units" disabled={isNew} onClick={(e) => onClickTab("packagingunits")}>
            {
              displayPackagingUnits && <StockItemPackagingUnitsTable
                setPackagingUnits={setEditablePackagingUnits}
                items={editablePackagingUnits} canEdit={canEdit}
                setSelectedTab={setSelectedTab}
                actions={{ onSave: handleSavePackagingUnit, onGoBack: handleGoBack, onRemoveItem: handleRemovePackagingUnit }}
                errors={itemValidity}
                setItemValidity={setItemValidity}
                validateItems={validatePackagingUnits}
              />
            }
          </Tab>
          <Tab label={"Transactions"} disabled={isNew} onClick={(e) => onClickTab("transactions")}>
            {
              displayTransactions && <StockItemTransactionsTable stockItemUuid={editableModel?.uuid ?? ""} partyList={partyLookupList} />
            }
          </Tab>
          <Tab label={"Batch Information"} disabled={isNew} onClick={(e) => onClickTab("batch")}>
            {
              displayBatchInformation && <StockItemBatchInformationTable stockItemUuid={editableModel?.uuid ?? ""} partyList={partyLookupList} />
            }
          </Tab>
          <Tab label={"Quantities"} disabled={isNew} onClick={(e) => onClickTab("quantities")}>
            {
              displayQuantities && <StockItemQuantitiesTable stockItemUuid={editableModel?.uuid ?? ""} partyList={partyLookupList} />
            }
          </Tab>
          <Tab label={"Stock Rules"} disabled={isNew} onClick={(e) => onClickTab("stockrules")}>
            {
              displayStockRules && <StockRulesTable partyLookupList={partyLookupUpdateableList}
                stockItemUuid={editableModel?.uuid!}
                errors={ruleValidity}
                setSelectedTab={setSelectedTab}
                partyFilterList={partyLookupList}
                canEdit={canEdit && partyLookupUpdateableList && partyLookupUpdateableList.length > 0}
                stockItemPackagingUnits={packagingUnits}
                setItemValidity={setRuleValidity}
                actions={{
                  onGoBack: handleGoBack,
                  onRemoveItem: handleRemoveStockRule
                }} setShowSplash={setShowSplash} />
            }
          </Tab>
          <Tab label="Reference" disabled={isNew} onClick={(e) => onClickTab("stockreference")}>
            {
                displayReferences && <StockItemReferenceTable
                    setReferences={setEditableReferences}
                    items={editableReferences} canEdit={canEdit}
                    setSelectedTab={setSelectedTab}
                    actions={{ onSave: handleSaveReference, onGoBack: handleGoBack, onRemoveItem: handleRemoveReference }}
                    errors={itemValidity}
                    setItemValidity={setItemValidity}
                    validateItems={validateReferences}
                />
            }
          </Tab>

        </Tabs>

      </div>
    </div>
  </>
}