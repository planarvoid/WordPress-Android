package com.soundcloud.groupie;

import static com.google.common.truth.Truth.assertAbout;
import static com.google.testing.compile.JavaSourceSubjectFactory.javaSource;
import static com.google.testing.compile.JavaSourcesSubjectFactory.javaSources;

import com.google.common.base.Joiner;
import com.google.testing.compile.JavaFileObjects;
import org.junit.Test;

import javax.tools.JavaFileObject;
import java.util.Arrays;

public class ProcessorTest {

    @Test
    public void verifyCompilesCorrectly() throws Exception {
        JavaFileObject input = JavaFileObjects.forSourceString("com/test/ValidExperiment", Joiner.on("\n").join(
                "package com.test;",
                "",
                "import com.soundcloud.groupie.ExperimentConfiguration;",
                "import com.soundcloud.groupie.ActiveExperiment;",
                "",
                "import java.util.Arrays;",
                "",
                "@ActiveExperiment",
                "public class ValidExperiment {",
                "",
                "    public static final ExperimentConfiguration CONFIGURATION = ExperimentConfiguration",
                "            .fromName(\"test\",",
                "                      \"name\",",
                "                      Arrays.asList(\"variant1\", \"variant2\"));",
                "}"
                                                               )
        );

        JavaFileObject expected = JavaFileObjects.forSourceString("com.soundcloud.android.experiments.ActiveExperiments", Joiner.on("\n").join(
                "package com.soundcloud.android.experiments;",
                "",
                "import com.soundcloud.groupie.ExperimentConfiguration;",
                "import com.test.ValidExperiment;",
                "import java.util.Arrays;",
                "import java.util.List;",
                "",
                "public final class ActiveExperiments {",
                "  public static final List<ExperimentConfiguration> ACTIVE_EXPERIMENTS = Arrays.asList(",
                "  ValidExperiment.CONFIGURATION",
                "  );",
                "}"
                                                                  )
        );

        assertAbout(javaSource())
                .that(input)
                .processedWith(new ExperimentProcessor())
                .compilesWithoutError()
                .and().generatesSources(expected);
    }

    @Test
    public void verifyStaticCheckErrors() throws Exception {
        JavaFileObject input = JavaFileObjects.forSourceString("com/test/ValidExperiment", Joiner.on("\n").join(
                "package com.test;",
                "",
                "import com.soundcloud.groupie.ExperimentConfiguration;",
                "import com.soundcloud.groupie.ActiveExperiment;",
                "",
                "import java.util.Arrays;",
                "",
                "@ActiveExperiment",
                "public class ValidExperiment {",
                "",
                "    public final ExperimentConfiguration CONFIGURATION = ExperimentConfiguration",
                "            .fromName(\"test\",",
                "                      \"name\",",
                "                      Arrays.asList(\"variant1\", \"variant2\"));",
                "}"
                                                               )
        );

        assertAbout(javaSource())
                .that(input)
                .processedWith(new ExperimentProcessor())
                .failsToCompile();
    }

    @Test
    public void verifyPublicCheckErrors() throws Exception {
        JavaFileObject input = JavaFileObjects.forSourceString("com/test/ValidExperiment", Joiner.on("\n").join(
                "package com.test;",
                "",
                "import com.soundcloud.groupie.ExperimentConfiguration;",
                "import com.soundcloud.groupie.ActiveExperiment;",
                "",
                "import java.util.Arrays;",
                "",
                "@ActiveExperiment",
                "public class ValidExperiment {",
                "",
                "    static final ExperimentConfiguration CONFIGURATION = ExperimentConfiguration",
                "            .fromName(\"test\",",
                "                      \"name\",",
                "                      Arrays.asList(\"variant1\", \"variant2\"));",
                "}"
                                                               )
        );

        assertAbout(javaSource())
                .that(input)
                .processedWith(new ExperimentProcessor())
                .failsToCompile();
    }

    @Test
    public void verifyNoConfigurationThrowsError() throws Exception {
        JavaFileObject input = JavaFileObjects.forSourceString("com/test/ValidExperiment", Joiner.on("\n").join(
                "package com.test;",
                "",
                "import com.soundcloud.groupie.ExperimentConfiguration;",
                "import com.soundcloud.groupie.ActiveExperiment;",
                "",
                "import java.util.Arrays;",
                "",
                "@ActiveExperiment",
                "public class ValidExperiment {",
                "",
                "    public static final boolean CONFIGURATION = true;",
                "}"
                                                               )
        );

        assertAbout(javaSource())
                .that(input)
                .processedWith(new ExperimentProcessor())
                .failsToCompile();
    }

    @Test
    public void verifyMultipleInputFilesCompilesCorrectly() throws Exception {
        JavaFileObject input1 = JavaFileObjects.forSourceString("com/test/ValidExperiment", Joiner.on("\n").join(
                "package com.test;",
                "",
                "import com.soundcloud.groupie.ExperimentConfiguration;",
                "import com.soundcloud.groupie.ActiveExperiment;",
                "",
                "import java.util.Arrays;",
                "",
                "@ActiveExperiment",
                "public class ValidExperiment {",
                "",
                "    public static final ExperimentConfiguration CONFIGURATION = ExperimentConfiguration",
                "            .fromName(\"test\",",
                "                      \"name\",",
                "                      Arrays.asList(\"variant1\", \"variant2\"));",
                "}"
                                                                )
        );

        JavaFileObject input2 = JavaFileObjects.forSourceString("com/test/ValidExperiment2", Joiner.on("\n").join(
                "package com.test;",
                "",
                "import com.soundcloud.groupie.ExperimentConfiguration;",
                "import com.soundcloud.groupie.ActiveExperiment;",
                "",
                "import java.util.Arrays;",
                "",
                "@ActiveExperiment",
                "public class ValidExperiment2 {",
                "",
                "    public static final ExperimentConfiguration CONFIGURATION = ExperimentConfiguration",
                "            .fromName(\"test\",",
                "                      \"name\",",
                "                      Arrays.asList(\"variant1\", \"variant2\"));",
                "}"
                                                                )
        );

        JavaFileObject expected = JavaFileObjects.forSourceString("com.soundcloud.android.experiments.ActiveExperiments", Joiner.on("\n").join(
                "package com.soundcloud.android.experiments;",
                "",
                "import com.soundcloud.groupie.ExperimentConfiguration;",
                "import com.test.ValidExperiment;",
                "import com.test.ValidExperiment2;",
                "import java.util.Arrays;",
                "import java.util.List;",
                "",
                "public final class ActiveExperiments {",
                "  public static final List<ExperimentConfiguration> ACTIVE_EXPERIMENTS = Arrays.asList(",
                "  ValidExperiment.CONFIGURATION,",
                "  ValidExperiment2.CONFIGURATION",
                "  );",
                "}"
                                                                  )
        );

        assertAbout(javaSources())
                .that(Arrays.asList(input1, input2))
                .processedWith(new ExperimentProcessor())
                .compilesWithoutError()
                .and().generatesSources(expected);
    }

    @Test
    public void withoutAnnotationDoesntGenerateAnything() throws Exception {
        JavaFileObject input = JavaFileObjects.forSourceString("com/test/ValidExperiment", Joiner.on("\n").join(
                "package com.test;",
                "",
                "import com.soundcloud.groupie.ExperimentConfiguration;",
                "import com.soundcloud.groupie.ActiveExperiment;",
                "",
                "import java.util.Arrays;",
                "",
                "public class ValidExperiment {",
                "",
                "    public static final ExperimentConfiguration CONFIGURATION = ExperimentConfiguration",
                "            .fromName(\"test\",",
                "                      \"name\",",
                "                      Arrays.asList(\"variant1\", \"variant2\"));",
                "}"
                                                               )
        );

        JavaFileObject expected = JavaFileObjects.forSourceString("com.soundcloud.android.experiments.ActiveExperiments", Joiner.on("\n").join(
                "package com.soundcloud.android.experiments;",
                "",
                "import com.soundcloud.groupie.ExperimentConfiguration;",
                "import java.util.Arrays;",
                "import java.util.List;",
                "",
                "public final class ActiveExperiments {",
                "  public static final List<ExperimentConfiguration> ACTIVE_EXPERIMENTS = Arrays.asList(",
                "  );",
                "}"
                                                                  )
        );

        assertAbout(javaSource())
                .that(input)
                .processedWith(new ExperimentProcessor())
                .compilesWithoutError()
                .and().generatesSources(expected);
    }
}
