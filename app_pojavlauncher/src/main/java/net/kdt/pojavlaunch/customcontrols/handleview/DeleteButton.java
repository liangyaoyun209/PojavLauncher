package net.kdt.pojavlaunch.customcontrols.handleview;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.Nullable;

import net.kdt.pojavlaunch.R;
import net.kdt.pojavlaunch.customcontrols.buttons.ControlButton;
import net.kdt.pojavlaunch.customcontrols.buttons.ControlInterface;

@SuppressLint("AppCompatCustomView")
public class DeleteButton extends Button implements ActionButtonInterface {
    public DeleteButton(Context context) {super(context); init();}
    public DeleteButton(Context context, @Nullable AttributeSet attrs) {super(context, attrs); init();}

    public void init() {
        setOnClickListener(this);
        setText("DELETE");

    }

    private ControlInterface mCurrentlySelectedButton = null;

    @Override
    public boolean shouldBeVisible() {
        return mCurrentlySelectedButton != null;
    }

    @Override
    public void setFollowedView(ControlInterface view) {
        mCurrentlySelectedButton = view;
    }

    @Override
    public void onClick() {
        if(mCurrentlySelectedButton == null) return;

        mCurrentlySelectedButton.removeButton();
    }
}
