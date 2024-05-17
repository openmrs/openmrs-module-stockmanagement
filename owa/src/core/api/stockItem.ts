import { InventoryGroupBy, StockItemDTO } from './types/stockItem/StockItem'
import { api, ResourceFilterCriteria, toQueryParams } from './api'
import {
  StockItemsTag,
  LIST_ID,
  StockItemPackagingUOMTag,
  StockItemTransactionsTag,
  StockItemInventoryTag,
  StockRulesTag,
  StockItemReferenceTag
} from './tagTypes'
import { BASE_OPENMRS_APP_URL } from '../../config';
import { PageableResult } from './types/PageableResult';
import { StockItemPackagingUOMDTO } from './types/stockItem/StockItemPackagingUOM';
import { StockBatchDTO } from './types/stockItem/StockBatchDTO';
import { StockItemTransactionDTO } from './types/stockItem/StockItemTransaction';
import { StockItemInventory } from './types/stockItem/StockItemInventory';
import { ImportResult } from './types/stockItem/ImportResult';
import { StockOperationItemCost } from './types/stockOperation/StockOperationItemCost';
import { StockRule } from './types/stockItem/StockRule';
import {StockItemReferenceDTO} from "./types/stockItem/StockItemReference";

export interface StockItemFilter extends ResourceFilterCriteria {
  isDrug?: string | null | undefined;
  drugUuid?: string | null;
  conceptUuid?: string | null;
}

export interface StockItemTransactionFilter extends ResourceFilterCriteria {
  stockItemUuid?: string | null;
  partyUuid?: string | null;
  stockOperationUuid?: string | null;
  includeBatchNo?: boolean | null;
  dateMin?: string | null;
  dateMax?: string | null;
  stockBatchUuid?: string | null | undefined;
}

export interface StockItemInventoryFilter extends ResourceFilterCriteria {
  stockItemUuid?: string | null;
  partyUuid?: string | null;
  locationUuid?: string | null;
  includeBatchNo?: boolean | null;
  stockBatchUuid?: string | null;
  groupBy?: InventoryGroupBy | null;
  totalBy?: InventoryGroupBy | null;
  stockOperationUuid?: string | null;
  date?: string | null;
  includeStockItemName?: "true" | "false" | "0" | "1";
  excludeExpired?: boolean | null;
}

export interface StockItemPackagingUOMFilter extends ResourceFilterCriteria {
  stockItemUuid?: string | null | undefined;
}


export interface StockItemReferenceFilter extends ResourceFilterCriteria {
  stockItemUuid?: string | null | undefined;
}

export interface StockBatchFilter extends ResourceFilterCriteria {
  stockItemUuid?: string | null | undefined;
  locationUuid?: string | null;
  excludeExpired?: boolean | null;
  excludeEmptyStock: boolean | null;
}

export interface StockInventoryResult extends PageableResult<StockItemInventory> {
  total: StockItemInventory[];
}

export interface StockRuleFilter extends ResourceFilterCriteria {
  stockItemUuid?: string | null;
  locationUuid?: string | null;
}



const stockItemsApi = api.injectEndpoints({
  endpoints: (build) => ({
    getStockItems: build.query<PageableResult<StockItemDTO>, StockItemFilter>({
      query: (filter) => ({
        url: `${BASE_OPENMRS_APP_URL}ws/rest/v1/stockmanagement/stockitem${toQueryParams(filter)}`,
        method: 'GET'
      }),
      providesTags: (_result, _err, id) => {

        return [{ type: StockItemsTag, id: LIST_ID }];
      },
      transformResponse: (response: PageableResult<StockItemDTO>, meta, arg) => {
        response?.results?.sort((a, b) => (a?.drugName ?? a.conceptName ?? "")?.localeCompare(b?.drugName ?? b.conceptName ?? ""));
        return response;
      }
    }),
    getStockItemTransactions: build.query<PageableResult<StockItemTransactionDTO>, StockItemTransactionFilter>({
      query: (filter) => ({
        url: `${BASE_OPENMRS_APP_URL}ws/rest/v1/stockmanagement/stockitemtransaction${toQueryParams(filter)}`,
        method: 'GET'
      }),
      providesTags: (_result, _err, id) => {

        return [{ type: StockItemTransactionsTag, id: LIST_ID }];
      }
    }),
    getStockItemInventory: build.query<StockInventoryResult, StockItemInventoryFilter>({
      query: (filter) => ({
        url: `${BASE_OPENMRS_APP_URL}ws/rest/v1/stockmanagement/stockiteminventory${toQueryParams(filter)}`,
        method: 'GET'
      }),
      providesTags: (_result, _err, id) => {
        return [{ type: StockItemInventoryTag, id: LIST_ID }];
      }
    }),
    getStockOperationItemsCost: build.query<PageableResult<StockOperationItemCost>, string>({
      query: (filter) => ({
        url: `${BASE_OPENMRS_APP_URL}ws/rest/v1/stockmanagement/stockoperationitemcost?v=default&stockOperationUuid=${filter}`,
        method: 'GET'
      }),
      providesTags: (_result, _err, id) => {
        return [{ type: StockItemInventoryTag, id: LIST_ID }];
      }
    }),
    getStockBatches: build.query<PageableResult<StockBatchDTO>, StockBatchFilter>({
      query: (filter) => ({
        url: `${BASE_OPENMRS_APP_URL}ws/rest/v1/stockmanagement/stockbatch${toQueryParams(filter)}`,
        method: 'GET'
      }),
      providesTags: (_result, _err, id) => {
        return [{ type: StockItemPackagingUOMTag, id: LIST_ID }];
      },
      transformResponse: (response: PageableResult<StockBatchDTO>, meta, arg) => {
        response?.results?.sort((a, b) => (a?.batchNo?.localeCompare(b?.batchNo)) ?? 0);
        return response;
      }
    }),

    getStockItemPackagingUOMs: build.query<PageableResult<StockItemPackagingUOMDTO>, StockItemPackagingUOMFilter>({
      query: (filter) => ({
        url: `${BASE_OPENMRS_APP_URL}ws/rest/v1/stockmanagement/stockitempackaginguom${toQueryParams(filter)}`,
        method: 'GET'
      }),
      providesTags: (_result, _err, id) => {
        return [{ type: StockItemPackagingUOMTag, id: LIST_ID }];
      },
      transformResponse: (response: PageableResult<StockItemPackagingUOMDTO>, meta, arg) => {
        response?.results?.sort((a, b) => (a?.packagingUomName?.localeCompare(b?.packagingUomName ?? "")) ?? 0);
        return response;
      }
    }),

    getStockItemReferences: build.query<PageableResult<StockItemReferenceDTO>, StockItemReferenceFilter>({
      query: (filter) => ({
        url: `${BASE_OPENMRS_APP_URL}ws/rest/v1/stockmanagement/stockitemreference${toQueryParams(filter)}`,
        method: 'GET'
      }),
      providesTags: (_result, _err, id) => {
        return [{ type: StockItemReferenceTag, id: LIST_ID }];
      },
      transformResponse: (response: PageableResult<StockItemReferenceDTO>, meta, arg) => {
        response?.results?.sort((a, b) => (a?.stockSourceName?.localeCompare(b?.stockSourceName ?? "")) ?? 0);
        return response;
      }
    }),

    getStockItem: build.query<StockItemDTO, string>({
      query: (id) => ({
        url: `${BASE_OPENMRS_APP_URL}ws/rest/v1/stockmanagement/stockitem/${id}?v=full`,
        method: 'GET'
      }),
      providesTags: (_result, _err, id) => {

        return [{ type: StockItemsTag, id }];
      }
    }),
    deleteStockItems: build.mutation<void, string[]>({
      query: (ids) => {

        let otherIds = ids.reduce((p, c, i) => {
          if (i === 0) return p;
          p += (p.length > 0 ? "," : "") + encodeURIComponent(c);
          return p;
        }, "");
        if (otherIds.length > 0) {
          otherIds = "?ids=" + otherIds;
        }
        return {
          url: `${BASE_OPENMRS_APP_URL}ws/rest/v1/stockmanagement/stockitem/${ids[0]}${otherIds}`,
          method: 'DELETE'
        }
      },
      invalidatesTags: (_result, _err, id) => {
        return [{ type: StockItemsTag }];
      }
    }),
    deleteStockItemPackagingUnit: build.mutation<void, string>({
      query: (id) => {
        return {
          url: `${BASE_OPENMRS_APP_URL}ws/rest/v1/stockmanagement/stockitempackaginguom/${id}`,
          method: 'DELETE'
        }
      },
      invalidatesTags: (_result, _err, id) => {
        return [{ type: StockItemPackagingUOMTag }];
      }
    }),
    deleteStockItemReference: build.mutation<void, string>({
      query: (id) => {
        return {
          url: `${BASE_OPENMRS_APP_URL}ws/rest/v1/stockmanagement/stockitemreference/${id}`,
          method: 'DELETE'
        }
      },
      invalidatesTags: (_result, _err, id) => {
        return [{ type: StockItemReferenceTag }];
      }
    }),
    createStockItem: build.mutation<void, StockItemDTO>({
      query: (stockItem) => {
        return {
          url: `${BASE_OPENMRS_APP_URL}ws/rest/v1/stockmanagement/stockitem`,
          method: 'POST',
          body: stockItem
        }
      },
      invalidatesTags: (_result, _err, id) => {
        return [{ type: StockItemsTag }];
      }
    }),
    updateStockItem: build.mutation<void, { model: StockItemDTO, uuid: string }>({
      query: (stockItem) => {
        return {
          url: `${BASE_OPENMRS_APP_URL}ws/rest/v1/stockmanagement/stockitem/${stockItem.uuid}`,
          method: 'POST',
          body: stockItem.model
        }
      },
      invalidatesTags: (_result, _err, id) => {
        return [{ type: StockItemsTag, id: id.uuid }];
      }
    }),
    createStockItemPackagingUnit: build.mutation<void, StockItemDTO>({
      query: (uom) => {
        return {
          url: `${BASE_OPENMRS_APP_URL}ws/rest/v1/stockmanagement/stockitempackaginguom`,
          method: 'POST',
          body: uom
        }
      },
      invalidatesTags: (_result, _err, id) => {
        return [{ type: StockItemPackagingUOMTag }];
      }
    }),
    createStockItemReference: build.mutation<void, StockItemReferenceDTO>({
      query: (stockItemReference) => {
        return {
          url: `${BASE_OPENMRS_APP_URL}ws/rest/v1/stockmanagement/stockitemreference`,
          method: 'POST',
          body: stockItemReference
        }
      },
      invalidatesTags: (_result, _err, id) => {
        return [{ type: StockItemReferenceTag }];
      }
    }),
    updateStockItemPackagingUnit: build.mutation<void, { model: StockItemDTO, uuid: string }>({
      query: (uom) => {
        return {
          url: `${BASE_OPENMRS_APP_URL}ws/rest/v1/stockmanagement/stockitempackaginguom/${uom.uuid}`,
          method: 'POST',
          body: uom.model
        }
      },
      invalidatesTags: (_result, _err, id) => {
        return [{ type: StockItemPackagingUOMTag, id: id.uuid }];
      }
    }),
    updateStockItemReference: build.mutation<void, { model: StockItemReferenceDTO, uuid: string }>({
      query: (stockItemReference) => {
        return {
          url: `${BASE_OPENMRS_APP_URL}ws/rest/v1/stockmanagement/stockitemreference/${stockItemReference.uuid}`,
          method: 'POST',
          body: stockItemReference.model
        }
      },
      invalidatesTags: (_result, _err, id) => {
        return [{ type: StockItemPackagingUOMTag, id: id.uuid }];
      }
    }),
    importStockItem: build.mutation<ImportResult, FormData>({
      query: (formData) => {
        return {
          url: `${BASE_OPENMRS_APP_URL}ws/rest/v1/stockmanagement/stockitemimport`,
          method: 'POST',
          body: formData
        }
      },
      invalidatesTags: (_result, _err, id) => {
        return [{ type: StockItemsTag }];
      }
    }),
    getStockRules: build.query<PageableResult<StockRule>, StockRuleFilter>({
      query: (filter) => ({
        url: `${BASE_OPENMRS_APP_URL}ws/rest/v1/stockmanagement/stockrule${toQueryParams(filter)}`,
        method: 'GET'
      }),
      providesTags: (_result, _err, id) => {

        return [{ type: StockRulesTag, id: LIST_ID }];
      }
    }),
    createStockRule: build.mutation<void, StockRule>({
      query: (uom) => {
        return {
          url: `${BASE_OPENMRS_APP_URL}ws/rest/v1/stockmanagement/stockrule`,
          method: 'POST',
          body: uom
        }
      },
      invalidatesTags: (_result, _err, id) => {
        return [{ type: StockRulesTag }];
      }
    }),
    updateStockRule: build.mutation<void, { model: StockRule, uuid: string }>({
      query: (uom) => {
        return {
          url: `${BASE_OPENMRS_APP_URL}ws/rest/v1/stockmanagement/stockrule/${uom.uuid}`,
          method: 'POST',
          body: uom.model
        }
      },
      invalidatesTags: (_result, _err, id) => {
        return [{ type: StockRulesTag, id: id.uuid }];
      }
    }),
    deleteStockRule: build.mutation<void, string>({
      query: (id) => {
        return {
          url: `${BASE_OPENMRS_APP_URL}ws/rest/v1/stockmanagement/stockrule/${id}`,
          method: 'DELETE'
        }
      },
      invalidatesTags: (_result, _err, id) => {
        return [{ type: StockRulesTag }];
      }
    }),
  }),
  overrideExisting: false,
})

export const {
  useGetStockItemsQuery,
  useLazyGetStockItemsQuery,
  useDeleteStockItemsMutation,
  useGetStockItemQuery,
  useLazyGetStockItemQuery,
  useCreateStockItemMutation,
  useUpdateStockItemMutation,
  useLazyGetStockItemPackagingUOMsQuery,
  useGetStockItemReferencesQuery,
  useGetStockItemPackagingUOMsQuery,
  useLazyGetStockBatchesQuery,
  useGetStockBatchesQuery,
  useLazyGetStockItemTransactionsQuery,
  useGetStockItemTransactionsQuery,
  useGetStockItemInventoryQuery,
  useLazyGetStockItemInventoryQuery,
  useCreateStockItemPackagingUnitMutation,
  useUpdateStockItemPackagingUnitMutation,
  useDeleteStockItemPackagingUnitMutation,
  useCreateStockItemReferenceMutation,
  useUpdateStockItemReferenceMutation,
  useDeleteStockItemReferenceMutation,
  useImportStockItemMutation,
  useGetStockOperationItemsCostQuery,
  useLazyGetStockOperationItemsCostQuery,
  useGetStockRulesQuery,
  useLazyGetStockRulesQuery,
  useCreateStockRuleMutation,
  useUpdateStockRuleMutation,
  useDeleteStockRuleMutation
} = stockItemsApi