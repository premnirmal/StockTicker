package com.github.premnirmal.ticker.network;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import retrofit.converter.ConversionException;
import retrofit.converter.Converter;
import retrofit.converter.GsonConverter;
import retrofit.mime.TypedInput;
import retrofit.mime.TypedOutput;

/**
 * Created by premnirmal on 12/21/14.
 */
public class StupidYahooWrapConverter implements Converter {

    private static final Pattern PATTERN_RESPONSE = Pattern.compile("YAHOO\\.Finance\\.SymbolSuggest\\.ssCallback\\((\\{.*?\\})\\)");
    private final Gson gson = new GsonBuilder().create();

    @Override
    public Object fromBody(TypedInput body, Type type) throws
            ConversionException {
        try {
            final String bodyString = getString(body.in());
            final Matcher m = PATTERN_RESPONSE.matcher(bodyString);
            if (m.find()) {
                final Suggestions suggestions = gson.fromJson(m.group(1), Suggestions.class);
                return suggestions;
            }
            throw new RuntimeException("Invalid response");
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public TypedOutput toBody(Object object) {
        return new GsonConverter(gson).toBody(object);
    }

    public String getString(InputStream is) throws IOException {
        int ch;
        StringBuilder sb = new StringBuilder();
        while ((ch = is.read()) != -1)
            sb.append((char) ch);
        return sb.toString();
    }
}
