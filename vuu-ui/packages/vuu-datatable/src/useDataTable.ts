import {
  DataSource,
  DataSourceConfigMessage,
  DataSourceRow,
  DataSourceSubscribedMessage,
} from "@finos/vuu-data";
import {
  ColumnDescriptor,
  GridConfig,
  KeyedColumnDescriptor,
  TypeFormatting,
} from "@finos/vuu-datagrid-types";
import { applySort, metadataKeys, roundDecimal } from "@finos/vuu-utils";
import { useCallback, useMemo, useState } from "react";
import {
  TableColumnResizeHandler,
  ValueFormatter,
  ValueFormatters,
} from "./dataTableTypes";
import { KeySet } from "./KeySet";
import { useTableModel } from "./useTableModel";
import { useDataSource } from "./useDataSource";
import { VuuSortType } from "@finos/vuu-protocol-types";

export interface DataTableHookProps {
  config: GridConfig;
  data?: DataSourceRow[];
  dataSource?: DataSource;
  onConfigChange?: (config: GridConfig) => void;
}

const { KEY, IS_EXPANDED } = metadataKeys;
const DEFAULT_NUMERIC_FORMAT: TypeFormatting = {};
const defaultValueFormatter = (value: unknown) =>
  value == null ? "" : typeof value === "string" ? value : value.toString();
const numericFormatter = ({ align = "right", type }: ColumnDescriptor) => {
  if (type === undefined || typeof type === "string") {
    return defaultValueFormatter;
  } else {
    const {
      alignOnDecimals = false,
      decimals,
      zeroPad = false,
    } = type.formatting ?? DEFAULT_NUMERIC_FORMAT;
    return (value: unknown) => {
      if (
        typeof value === "string" &&
        (value.startsWith("Σ") || value.startsWith("["))
      ) {
        return value;
      }
      const number =
        typeof value === "number"
          ? value
          : typeof value === "string"
          ? parseFloat(value)
          : undefined;
      return roundDecimal(number, align, decimals, zeroPad, alignOnDecimals);
    };
  }
};

const getValueFormatter = (column: KeyedColumnDescriptor): ValueFormatter => {
  const { serverDataType } = column;
  if (serverDataType === "string" || serverDataType === "char") {
    return (value: unknown) => value as string;
  } else if (serverDataType === "double") {
    return numericFormatter(column);
  }
  return defaultValueFormatter;
};

export const useDataTable = ({
  config,
  data: dataProp,
  dataSource,
  onConfigChange,
}: DataTableHookProps) => {
  const keys = useMemo(() => new KeySet({ from: 0, to: 0 }), []);
  const [visibleRows, setVisibleRows] = useState<DataSourceRow[]>([]);
  const [rowCount, setRowCount] = useState<number>(dataProp?.length ?? 0);

  if (dataProp === undefined && dataSource === undefined) {
    throw Error("no data source provided to DataTable");
  }

  const onSizeChange = useCallback((size: number) => {
    setRowCount(size);
  }, []);

  const { columns, dispatchColumnAction } = useTableModel(config);

  const onSubscribed = useCallback(
    (subscription: DataSourceSubscribedMessage) => {
      if (subscription.tableMeta) {
        const { columns: columnNames, dataTypes: serverDataTypes } =
          subscription.tableMeta;
        dispatchColumnAction({
          type: "setTypes",
          columnNames,
          serverDataTypes,
        });
      }
    },
    [dispatchColumnAction]
  );

  const valueFormatters = useMemo(() => {
    return columns.reduce<ValueFormatters>(
      (map, column) => ((map[column.name] = getValueFormatter(column)), map),
      {}
    );
  }, [columns]);

  useMemo(() => {
    onConfigChange?.({
      ...config,
      columns,
    });
  }, [columns, config, onConfigChange]);

  useMemo(() => {
    dispatchColumnAction({ type: "init", config });
  }, [config, dispatchColumnAction]);

  const handleConfigChangeFromDataSource = useCallback(
    (message: DataSourceConfigMessage) => {
      switch (message.type) {
        case "groupBy":
          return dispatchColumnAction({
            type: "tableConfig",
            groupBy: message.groupBy,
          });
        case "filter":
          return dispatchColumnAction({
            type: "tableConfig",
            filter: message.filter,
          });
        case "sort":
          return dispatchColumnAction({
            type: "tableConfig",
            sort: message.sort,
          });
      }
    },
    [dispatchColumnAction]
  );

  const { data, setRange } = useDataSource({
    dataSource,
    onConfigChange: handleConfigChangeFromDataSource,
    onSubscribed,
    onSizeChange,
  });

  const setRangeVertical = useCallback(
    (from: number, to: number) => {
      if (dataSource) {
        setRange(from, to);
      } else {
        keys.reset({ from, to });
        const visibleRows = dataProp
          ? keys.withKeys(dataProp.slice(from, to))
          : [];
        setVisibleRows(visibleRows);
      }
    },
    [dataProp, dataSource, keys, setRange]
  );

  const handleSort = useCallback(
    (
      column: KeyedColumnDescriptor,
      extendSort = false,
      sortType?: VuuSortType
    ) => {
      if (dataSource) {
        dataSource.sort = applySort(
          dataSource.sort,
          column,
          extendSort,
          sortType
        );
      }
    },
    [dataSource]
  );

  const handleColumnResize: TableColumnResizeHandler = useCallback(
    (phase, columnName, width) => {
      const column = columns.find((column) => column.name === columnName);
      if (column) {
        dispatchColumnAction({
          type: "resizeColumn",
          phase,
          column,
          width,
        });
      } else {
        throw Error(
          `useDataTable.handleColumnResize, column ${columnName} not found`
        );
      }
    },
    [columns, dispatchColumnAction]
  );

  const handleToggleGroup = useCallback(
    (row: DataSourceRow) => {
      if (dataSource) {
        if (row[IS_EXPANDED]) {
          dataSource.closeTreeNode(row[KEY]);
        } else {
          dataSource.openTreeNode(row[KEY]);
        }
      }
    },
    [dataSource]
  );

  const handleRemoveColumnFromGroupBy = useCallback(
    (column: KeyedColumnDescriptor) => {
      if (dataSource && dataSource.groupBy.includes(column.name)) {
        dataSource.groupBy = dataSource.groupBy.filter(
          (columnName) => columnName !== column.name
        );
      }
    },
    [dataSource]
  );

  return {
    valueFormatters,
    columns,
    data: dataSource ? data : visibleRows,
    dispatchColumnAction,
    onColumnResize: handleColumnResize,
    onRemoveColumnFromGroupBy: handleRemoveColumnFromGroupBy,
    onSort: handleSort,
    onToggleGroup: handleToggleGroup,
    setRangeVertical,
    rowCount,
  };
};