package com.example.controller;

import com.example.dao.BaseDao;
import org.primefaces.model.FilterMeta;
import org.primefaces.model.LazyDataModel;
import org.primefaces.model.SortOrder;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class GenericLazyDataModel<T, ID extends Serializable> extends LazyDataModel<T> {
    private static final long serialVersionUID = 1L;
    private final BaseDao<T, ID> baseDao;
    private final Map<String, Object> exactMatchFilters;
    private transient Consumer<List<T>> onDataLoadCallback;

    public GenericLazyDataModel(BaseDao<T, ID> baseDao, Map<String, Object> exactMatchFilters) {
        this.baseDao = baseDao;
        this.exactMatchFilters = exactMatchFilters != null ? exactMatchFilters : new HashMap<>();
    }

    public void setOnDataLoadCallback(Consumer<List<T>> callback) {
        this.onDataLoadCallback = callback;
    }

    @Override
    public List<T> load(int first, int pageSize, String sortField, SortOrder sortOrder, Map<String, FilterMeta> filters) {
        try {
            int totalCount = baseDao.getTotalEntityCount(filters, exactMatchFilters);
            this.setRowCount(totalCount);

            if (totalCount == 0) {
                return new ArrayList<>();
            }

            List<T> data = baseDao.findPaginatedEntities(filters, exactMatchFilters, first, pageSize);
            if (onDataLoadCallback != null) {
                onDataLoadCallback.accept(data);
            }
            return data;
        } catch (Exception e) {
            e.printStackTrace();
            this.setRowCount(0);
            return new ArrayList<>();
        }
    }

    @Override
    public T getRowData(String rowKey) {
        try {
            return baseDao.findById((ID) Long.valueOf(rowKey));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public Object getRowKey(T entity) {
        try {
            return entity.getClass().getMethod("getId").invoke(entity);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}