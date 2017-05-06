package me.ghui.v2ex.htmlpicker.bind;

import org.jsoup.nodes.Element;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import me.ghui.v2ex.htmlpicker.Fruit;
import me.ghui.v2ex.htmlpicker.PickAdapter;
import me.ghui.v2ex.htmlpicker.PickAdapterFactory;
import me.ghui.v2ex.htmlpicker.PickFactory;
import me.ghui.v2ex.htmlpicker.annotations.Pick;
import me.ghui.v2ex.htmlpicker.internal.Types;
import me.ghui.v2ex.htmlpicker.reflect.TypeToken;

/**
 * Created by ghui on 13/04/2017.
 */

public final class ReflectivePickAdapterFactory implements PickAdapterFactory {

    @Override
    public <T> PickAdapter<T> create(Fruit picker, TypeToken<T> type) {
        Class<? super T> raw = type.getRawType();
        if (!Object.class.isAssignableFrom(raw)) {
            return null; // it's a primitive!
        }
        return new Adapter<>(type, getBoundFields(picker, type, raw));
    }

    private List<BoundField> getBoundFields(Fruit fruit, TypeToken<?> type, Class<?> raw) {
        List<BoundField> boundFields = new ArrayList<>();
        if (raw.isInterface()) return boundFields;
        //only support current class annotation(exclude the super class annotion)
        Pick classPick = raw.getAnnotation(Pick.class);
        while (raw != Object.class) {
            for (Field field : raw.getDeclaredFields()) {
                String name = field.getName();
                if (name.contains("$change") || name.equals("serialVersionUID") || field.isSynthetic()) {
                    continue;
                }
                field.setAccessible(true);
                Type fieldType = Types.resolve(type.getType(), raw, field.getGenericType());
                BoundField boundField = createBoundField(fruit, field, classPick, TypeToken.get(fieldType));
                boundFields.add(boundField);
            }
            type = TypeToken.get(Types.resolve(type.getType(), raw, raw.getGenericSuperclass()));
            raw = type.getRawType();
        }
        return boundFields;
    }

    private BoundField createBoundField(Fruit fruit, Field field, final Pick parentPick, final TypeToken<?> fieldType) {
        final PickAdapter<?> pickAdapter = fruit.getAdapter(fieldType);
        return new BoundField(field) {
            @Override
            public void read(Element element, Object instance) throws IllegalAccessException {
                Pick pick = field.getAnnotation(Pick.class);
                if (parentPick != null) {
                    if (pick == null) {
                        throw new IllegalArgumentException("ignore Field: " + field.getName() + " without a Pick anotation");
                    }
                    String query = parentPick.value() + " " + pick.value();//ancestor child
                    pick = PickFactory.create(query, pick.attr());
                }
                Object fieldValue = pickAdapter.read(element, pick);
                if (fieldValue != null) {
                    field.set(instance, fieldValue);
                }
            }
        };
    }

    private static abstract class BoundField {
        Field field;

        BoundField(Field field) {
            this.field = field;
        }

        public abstract void read(Element element, Object instance) throws IllegalAccessException;
    }

    private static final class Adapter<T> extends PickAdapter<T> {

        private TypeToken<T> type;
        private List<BoundField> boundFields;

        Adapter(TypeToken<T> type, List<BoundField> boundFields) {
            this.type = type;
            this.boundFields = boundFields;
        }

        @Override
        public T read(Element element, @Nullable Pick pick) {
            T instance = null;
            try {
                final Constructor<? super T> constructor
                        = type.getRawType().getDeclaredConstructor();
                if (!constructor.isAccessible()) {
                    constructor.setAccessible(true);
                }
                instance = (T) constructor.newInstance();
                for (BoundField boundField : boundFields) {
                    boundField.read(element, instance);
                }
            } catch (NoSuchMethodException
                    | IllegalAccessException
                    | InstantiationException
                    | InvocationTargetException e) {
                e.printStackTrace();
            }
            return instance;
        }
    }

}
