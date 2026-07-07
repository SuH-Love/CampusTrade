package com.campustrade.common;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class PageResult<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<T> list;
    private Long total;

    public PageResult() {}

    public PageResult(List<T> list, Long total) {
        this.list = list;
        this.total = total;
    }
}