package com.github.premnirmal.ticker.network;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.io.InputStream;

import retrofit.converter.GsonConverter;
import retrofit.mime.TypedOutput;

/**
 * Created by premnirmal on 4/10/15.
 */
abstract class BaseConverter implements retrofit.converter.Converter {

    protected final Gson gson = new GsonBuilder().create();

    protected String getString(InputStream is) throws IOException {
        int ch;
        StringBuilder sb = new StringBuilder();
        while ((ch = is.read()) != -1)
            sb.append((char) ch);
        return sb.toString();
    }

    @Override
    public TypedOutput toBody(Object object) {
        return new GsonConverter(gson).toBody(object);
    }
}
