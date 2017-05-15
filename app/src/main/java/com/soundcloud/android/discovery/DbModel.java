package com.soundcloud.android.discovery;

import com.google.auto.value.AutoValue;
import com.squareup.sqldelight.RowMapper;
import com.squareup.sqldelight.SqlDelightStatement;

final class DbModel {

    private static final UrnAdapter URN_ADAPTER = new UrnAdapter();
    private static final StringListAdapter STRING_LIST_ADAPTER = new StringListAdapter();

    private DbModel() {
    }

    @AutoValue
    public abstract static class DiscoveryCard implements DiscoveryCardModel {
        static final DiscoveryCardModel.Creator<DiscoveryCard> CREATOR = AutoValue_DbModel_DiscoveryCard::new;

        static final DiscoveryCardModel.Factory<DiscoveryCard> FACTORY = new DiscoveryCardModel.Factory<>(CREATOR);

        static final SqlDelightStatement SELECT_ALL = FACTORY.selectAll();
    }

    @AutoValue
    abstract static class FullDiscoveryCard implements DiscoveryCardModel.SelectAllModel<SingleContentSelectionCard, MultipleContentSelectionCard> {
        static final DiscoveryCardModel.SelectAllCreator<SingleContentSelectionCard, MultipleContentSelectionCard, FullDiscoveryCard> CREATOR = AutoValue_DbModel_FullDiscoveryCard::new;

        static final DiscoveryCardModel.SelectAllMapper<DbModel.SingleContentSelectionCard, DbModel.MultipleContentSelectionCard, DbModel.FullDiscoveryCard> MAPPER = DbModel.DiscoveryCard.FACTORY
                .selectAllMapper(DbModel.FullDiscoveryCard.CREATOR,
                                 DbModel.SingleContentSelectionCard.FACTORY,
                                 DbModel.MultipleContentSelectionCard.FACTORY);
    }

    @AutoValue
    abstract static class SelectionItem implements SelectionItemModel {
        static final SelectionItemModel.Creator<SelectionItem> CREATOR = AutoValue_DbModel_SelectionItem::new;

        static final SelectionItemModel.Factory<SelectionItem> FACTORY = new SelectionItemModel.Factory<>(CREATOR, URN_ADAPTER);

        static final SqlDelightStatement SELECT_ALL_FOR_DISCOVERY_CARDS = FACTORY.selectForDiscoveryCards();

        static final RowMapper<SelectionItem> MAPPER = FACTORY.selectAllMapper();
    }

    @AutoValue
    abstract static class SingleContentSelectionCard implements SingleContentSelectionCardModel {
        static final SingleContentSelectionCardModel.Creator<SingleContentSelectionCard> CREATOR = AutoValue_DbModel_SingleContentSelectionCard::new;

        static final SingleContentSelectionCardModel.Factory<SingleContentSelectionCard> FACTORY = new SingleContentSelectionCardModel.Factory<>(CREATOR,
                                                                                                                                                 URN_ADAPTER,
                                                                                                                                                 URN_ADAPTER,
                                                                                                                                                 STRING_LIST_ADAPTER);
    }

    @AutoValue
    abstract static class MultipleContentSelectionCard implements MultipleContentSelectionCardModel {
        static final MultipleContentSelectionCardModel.Creator<MultipleContentSelectionCard> CREATOR = AutoValue_DbModel_MultipleContentSelectionCard::new;

        static final MultipleContentSelectionCardModel.Factory<MultipleContentSelectionCard> FACTORY = new MultipleContentSelectionCardModel.Factory<>(CREATOR, URN_ADAPTER, URN_ADAPTER);
    }
}
