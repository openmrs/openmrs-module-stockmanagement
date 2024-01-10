import { BaseOpenmrsData } from '../BaseOpenmrsData'
import { StockItem } from './StockItem';
import {StockSource} from "../stockOperation/StockSource";

export interface StockItemReference extends BaseOpenmrsData {
    referenceCode: string;
    stockSource: StockSource;
    stockItem: StockItem;
}

export interface StockItemReferenceDTO{
    id?: string;
    uuid?: string;
    stockItemUuid?: string;
    stockSourceName?: string;
    stockSourceUuid?: string;
    referenceCode?: string | null;
}


