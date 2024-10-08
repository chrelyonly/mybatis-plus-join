package com.github.yulichang.test.join.unit;

import com.github.yulichang.test.join.entity.TableA;
import com.github.yulichang.test.join.entity.UserDO;
import com.github.yulichang.test.util.Reset;
import com.github.yulichang.toolkit.JoinWrappers;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
public class TypeHandlerTest {

    @BeforeEach
    void setUp() {
        Reset.reset();
    }

    @Test
    void typeHandler() {
        MPJLambdaWrapper<UserDO> wrapper = JoinWrappers.lambda(UserDO.class);

        List<UserDO> list = wrapper.list();

        System.out.println(list.get(0).getJson().get(0).getClass().getName());

        list.forEach(System.out::println);
    }

    @Test
    void typeHandler1() {
        MPJLambdaWrapper<TableA> wrapper = JoinWrappers.lambda(TableA.class);

        List<TableA> list = wrapper.list();

        assert list.get(0).getListCol() != null;
        assert list.get(0).getEntryCol() != null;
        assert list.get(0).getMapCol() != null;

        list.forEach(System.out::println);
    }
}
