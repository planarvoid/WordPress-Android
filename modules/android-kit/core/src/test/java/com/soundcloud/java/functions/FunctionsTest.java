package com.soundcloud.java.functions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import com.soundcloud.java.test.SerializableTester;
import org.junit.Test;

public class FunctionsTest {

    @Test
    public void testIdentity_same() {
        Function<String, String> identity = Functions.identity();
        assertThat("foo").isSameAs(identity.apply("foo"));
    }

    @Test(expected = NullPointerException.class)
    public void testIdentityOfNullThrows() {
        Function<String, String> identity = Functions.identity();
        identity.apply(null);
    }

    @Test
    public void testIdentity_notSame() {
        Function<Object, Object> identity = Functions.identity();
        assertThat(new Object()).isNotSameAs(identity.apply(new Object()));
    }

    @Test
    public void testIdentitySerializable() {
        checkCanReserializeSingleton(Functions.identity());
    }

    @Test
    public void testToStringFunction_apply() {
        assertThat("3").isEqualTo(Functions.toStringFunction().apply(3));
        assertThat("hiya").isEqualTo(Functions.toStringFunction().apply("hiya"));
        assertThat("I'm a string").isEqualTo(
                Functions.toStringFunction().apply(
                        new Object() {
                            @Override
                            public String toString() {
                                return "I'm a string";
                            }
                        }));
    }

    @Test(expected = NullPointerException.class)
    public void testToStringFunctionThrowsNPEWhenInvokedWithNull() {
        Functions.toStringFunction().apply(null);
    }

    @Test
    public void testToStringFunctionSerializable() {
        checkCanReserializeSingleton(Functions.toStringFunction());
    }

    @Test
    public void testConstant() {
        Function<Object, Object> f = Functions.<Object>constant("correct");
        assertThat("correct").isEqualTo(f.apply(new Object()));

        Function<Object, String> g = Functions.constant(null);
        assertThat(g.apply(2)).isNull();
    }

    @Test(expected = NullPointerException.class)
    public void testConstantApplyingNullThrows() {
        Function<Object, String> g = Functions.constant(null);
        g.apply(null);
    }

    private static <Y> void checkCanReserializeSingleton(Function<? super String, Y> f) {
        Function<? super String, Y> g = SerializableTester.reserializeAndAssert(f);
        assertSame(f, g);
        for (Integer i = 1; i < 5; i++) {
            assertEquals(f.apply(i.toString()), g.apply(i.toString()));
        }
    }
}
