package com.github.yulichang.injector;

import com.baomidou.mybatisplus.core.injector.AbstractMethod;
import com.baomidou.mybatisplus.core.injector.AbstractSqlInjector;
import com.baomidou.mybatisplus.core.injector.DefaultSqlInjector;
import com.baomidou.mybatisplus.core.injector.ISqlInjector;
import com.baomidou.mybatisplus.core.injector.methods.*;
import com.baomidou.mybatisplus.core.mapper.Mapper;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.toolkit.ArrayUtils;
import com.baomidou.mybatisplus.core.toolkit.ExceptionUtils;
import com.github.yulichang.adapter.base.tookit.VersionUtils;
import com.github.yulichang.adapter.v3431.AbstractMethodV3431;
import com.github.yulichang.method.*;
import com.github.yulichang.toolkit.MPJTableMapperHelper;
import com.github.yulichang.toolkit.ReflectionKit;
import com.github.yulichang.toolkit.TableHelper;
import lombok.Getter;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.session.Configuration;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

/**
 * SQL 注入器
 *
 * @author yulichang
 * @see DefaultSqlInjector
 */
@Getter
public class MPJSqlInjector extends DefaultSqlInjector {

    private AbstractSqlInjector sqlInjector;

    public MPJSqlInjector() {
    }

    public MPJSqlInjector(ISqlInjector sqlInjector) {
        if (Objects.nonNull(sqlInjector) && sqlInjector instanceof AbstractSqlInjector) {
            this.sqlInjector = (AbstractSqlInjector) sqlInjector;
        }
    }

    /**
     * 升级到 mybatis plus 3.4.3.2 后对之前的版本兼容
     */
    @Deprecated
    @SuppressWarnings({"unused", "DeprecatedIsStillUsed"})
    public List<AbstractMethod> getMethodList(Class<?> mapperClass) {
        if (VersionUtils.compare(VersionUtils.getVersion(), "3.4.3.2") >= 0) {
            throw ExceptionUtils.mpe("DefaultSqlInjector 的 getMethodList(Class<?> mapperClass) 方法已在 3.4.3.2+ 改为" +
                    "getMethodList(Class<?> mapperClass, TableInfo tableInfo)\n");
        }
        if (Objects.nonNull(sqlInjector)) {
            List<AbstractMethod> methodList = AbstractMethodV3431.getMethod(sqlInjector, mapperClass);
            return methodFilter(methodList);
        } else {
            List<AbstractMethod> list = Stream.of(
                    new Insert(),
                    new DeleteByMap(),
                    new DeleteById(),
                    new DeleteBatchByIds(),
                    new UpdateById(),
                    new SelectById(),
                    new SelectBatchByIds(),
                    new SelectByMap()
            ).collect(toList());
            list.addAll(getWrapperMethod());
            list.addAll(getJoinMethod());
            return list;
        }
    }

    /**
     * mybatis plus 3.4.3.2
     * <p>
     * Deprecated
     * 3.5.6 getMethodList(Configuration, Class, TableInfo)
     */
    @Override
    @Deprecated
    @SuppressWarnings({"deprecation", "DeprecatedIsStillUsed"})
    public List<AbstractMethod> getMethodList(Class<?> mapperClass, TableInfo tableInfo) {
        if (Objects.nonNull(sqlInjector)) {
            return methodFilter(sqlInjector.getMethodList(mapperClass, tableInfo));
        }
        return methodFilter(super.getMethodList(mapperClass, tableInfo));
    }

    @Override
    public List<AbstractMethod> getMethodList(Configuration configuration, Class<?> mapperClass, TableInfo tableInfo) {
        if (Objects.nonNull(sqlInjector)) {
            return methodFilter(sqlInjector.getMethodList(configuration, mapperClass, tableInfo));
        }
        return methodFilter(super.getMethodList(configuration, mapperClass, tableInfo));
    }

    private List<AbstractMethod> methodFilter(List<AbstractMethod> list) {
        String packageStr = SelectList.class.getPackage().getName();
        List<String> methodList = Arrays.asList(
                "Update",
                "Delete",
                "SelectOne",
                "SelectCount",
                "SelectMaps",
                "SelectMapsPage",
                "SelectObjs",
                "SelectList",
                "SelectPage");
        list.removeIf(i -> methodList.contains(i.getClass().getSimpleName()) &&
                Objects.equals(packageStr, i.getClass().getPackage().getName()));
        addAll(list, getWrapperMethod());
        addAll(list, getJoinMethod());
        return list;
    }

    private List<AbstractMethod> getJoinMethod() {
        List<AbstractMethod> list = new ArrayList<>();
        if (VersionUtils.compare(VersionUtils.getVersion(), "3.5.0") >= 0) {
            list.add(new DeleteJoin(SqlMethod.DELETE_JOIN.getMethod()));
            list.add(new UpdateJoin(SqlMethod.UPDATE_JOIN.getMethod()));
            list.add(new UpdateJoinAndNull(SqlMethod.UPDATE_JOIN_AND_NULL.getMethod()));
            list.add(new SelectJoinCount(SqlMethod.SELECT_JOIN_COUNT.getMethod()));
            list.add(new SelectJoinOne(SqlMethod.SELECT_JOIN_ONE.getMethod()));
            list.add(new SelectJoinList(SqlMethod.SELECT_JOIN_LIST.getMethod()));
            list.add(new SelectJoinPage(SqlMethod.SELECT_JOIN_PAGE.getMethod()));
        } else {
            list.add(new DeleteJoin());
            list.add(new UpdateJoin());
            list.add(new UpdateJoinAndNull());
            list.add(new SelectJoinCount());
            list.add(new SelectJoinOne());
            list.add(new SelectJoinList());
            list.add(new SelectJoinPage());
        }
        return list;
    }

    private List<AbstractMethod> getWrapperMethod() {
        List<AbstractMethod> list = new ArrayList<>();
        list.add(new com.github.yulichang.method.mp.Delete());
        list.add(new com.github.yulichang.method.mp.SelectOne());
        list.add(new com.github.yulichang.method.mp.SelectCount());
        list.add(new com.github.yulichang.method.mp.SelectMaps());
        list.add(new com.github.yulichang.method.mp.SelectMapsPage());
        list.add(new com.github.yulichang.method.mp.SelectObjs());
        list.add(new com.github.yulichang.method.mp.SelectList());
        list.add(new com.github.yulichang.method.mp.SelectPage());
        list.add(new com.github.yulichang.method.mp.Update());
        return list;
    }

    private void addAll(List<AbstractMethod> source, List<AbstractMethod> addList) {
        for (AbstractMethod method : addList) {
            if (source.stream().noneMatch(m -> m.getClass().getSimpleName().equals(method.getClass().getSimpleName()))) {
                source.add(method);
            }
        }
    }

    @Override
    public void inspectInject(MapperBuilderAssistant builderAssistant, Class<?> mapperClass) {
        Class<?> modelClass = ReflectionKit.getSuperClassGenericType(mapperClass, Mapper.class, 0);
        super.inspectInject(builderAssistant, mapperClass);
        MPJTableMapperHelper.init(modelClass, mapperClass);
        Supplier<Class<?>> supplier = () -> {
            try {
                return extractModelClassOld(mapperClass);
            } catch (Throwable throwable) {
                return null;
            }
        };
        TableHelper.init(modelClass, supplier.get());
    }

    @SuppressWarnings("IfStatementWithIdenticalBranches")
    protected Class<?> extractModelClassOld(Class<?> mapperClass) {
        Type[] types = mapperClass.getGenericInterfaces();
        ParameterizedType target = null;
        for (Type type : types) {
            if (type instanceof ParameterizedType) {
                Type[] typeArray = ((ParameterizedType) type).getActualTypeArguments();
                if (ArrayUtils.isNotEmpty(typeArray)) {
                    for (Type t : typeArray) {
                        if (t instanceof TypeVariable || t instanceof WildcardType) {
                            break;
                        } else {
                            target = (ParameterizedType) type;
                            break;
                        }
                    }
                }
                break;
            }
        }
        return target == null ? null : (Class<?>) target.getActualTypeArguments()[0];
    }
}
