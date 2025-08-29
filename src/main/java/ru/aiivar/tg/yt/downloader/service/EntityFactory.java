package ru.aiivar.tg.yt.downloader.service;

import org.springframework.stereotype.Service;
import ru.aiivar.tg.yt.downloader.entity.BaseEntity;

import java.lang.reflect.InvocationTargetException;
import java.util.UUID;

@Service
public class EntityFactory {

    public <T extends BaseEntity> T newEntity(Class<T> clazz) {
        try {
            var newInstance = clazz.getDeclaredConstructor().newInstance();
            newInstance.setId(UUID.randomUUID().toString());
            return newInstance;
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            return null;
        }
    }
}
