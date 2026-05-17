package org.hameed.hameedmoneycli.proxy.dto;

import java.util.List;

public record TwelveDataListContainer<T>(
        List<T> data
) {
}
