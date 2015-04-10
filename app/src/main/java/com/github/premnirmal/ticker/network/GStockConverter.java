package com.github.premnirmal.ticker.network;

import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import retrofit.converter.ConversionException;
import retrofit.mime.TypedInput;

/**
 * Created by premnirmal on 4/10/15.
 */
class GStockConverter extends BaseConverter {

    @Override
    public Object fromBody(TypedInput body, Type type) throws ConversionException {
        try {
            final String bodyString = getString(body.in()).replaceAll("\n","");
            final String responseString;
            if (bodyString.startsWith("//")) {
                responseString = bodyString.substring(2, bodyString.length());
            } else {
                responseString = bodyString;
            }
            final Type collectionType = new TypeToken<List<GStock>>() {
            }.getType();
            final List<GStock> stocks = gson.fromJson(responseString, collectionType);
            return stocks;
        } catch (IOException e) {
            e.printStackTrace();
            return new ArrayList<GStock>();
        }
    }

}
