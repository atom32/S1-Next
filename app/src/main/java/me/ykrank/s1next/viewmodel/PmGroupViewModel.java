package me.ykrank.s1next.viewmodel;

import android.databinding.ObservableField;
import android.view.View;

import me.ykrank.s1next.data.api.model.PmGroup;
import me.ykrank.s1next.view.event.PmGroupClickEvent;
import me.ykrank.s1next.widget.RxBus;


public final class PmGroupViewModel {

    public final ObservableField<PmGroup> pmGroup = new ObservableField<>();

    public View.OnClickListener clickGroup(RxBus rxBus) {
        return v -> rxBus.post(new PmGroupClickEvent(pmGroup.get()));
    }
}
