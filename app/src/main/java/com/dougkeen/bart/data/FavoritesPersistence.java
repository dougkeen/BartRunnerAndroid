package com.dougkeen.bart.data;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;

import android.content.Context;
import android.util.Log;

import com.dougkeen.bart.BartRunnerApplication;
import com.dougkeen.bart.model.StationPair;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.googlecode.androidannotations.annotations.EBean;

@EBean
public class FavoritesPersistence {
    private static final String TAG = "FavoritesPersistence";

    private final ObjectMapper objectMapper = new ObjectMapper();

    private BartRunnerApplication app;

    public FavoritesPersistence(Context context) {
        app = (BartRunnerApplication) context.getApplicationContext();
    }

    public void persist(List<StationPair> favorites) {
        FileOutputStream outputStream = null;
        try {
            outputStream = app
                    .openFileOutput("favorites", Context.MODE_PRIVATE);
            objectMapper.writeValue(outputStream, favorites);
        } catch (Exception e) {
            Log.e(TAG, "Could not write favorites file", e);
        } finally {
            IOUtils.closeQuietly(outputStream);
        }
    }

    public List<StationPair> restore() {
        for (String file : app.fileList()) {
            if ("favorites".equals(file)) {
                FileInputStream inputStream = null;
                try {
                    inputStream = app.openFileInput("favorites");
                    return objectMapper.readValue(inputStream,
                            new TypeReference<ArrayList<StationPair>>() {
                            });
                } catch (Exception e) {
                    Log.e(TAG, "Could not read favorites file", e);
                } finally {
                    IOUtils.closeQuietly(inputStream);
                }
            }
        }

        return new ArrayList<StationPair>();
    }
}
