package operators;


import static com.soundcloud.android.coreutils.check.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.List;

public final class CollectionOperators {

    private CollectionOperators() {}

    /**
     * For a given list, applies a transformation eagerly and returns a new list containing the transformed
     * elements.
     * @param fromList The list containing the source elements
     * @param function The function to apply to the source elements
     * @return A new list containing the transformed elements
     */
    public static <F, T> List<T> transform(
            List<F> fromList, Function<? super F, ? extends T> function) {
        checkNotNull(fromList);
        checkNotNull(function);
        List<T> transformedList = new ArrayList<T>(fromList.size());
        for(F element : fromList) {
            transformedList.add(function.apply(element));
        }
        return transformedList;
    }



}
