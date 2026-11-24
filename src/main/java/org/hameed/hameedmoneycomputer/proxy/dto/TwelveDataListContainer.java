package org.hameed.hameedmoneycomputer.proxy.dto;

import java.util.List;

public record TwelveDataListContainer<T>(
        List<T> data
) {
}
