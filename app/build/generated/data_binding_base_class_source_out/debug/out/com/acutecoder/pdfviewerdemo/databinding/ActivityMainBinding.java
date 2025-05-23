// Generated by view binder compiler. Do not edit!
package com.acutecoder.pdfviewerdemo.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewbinding.ViewBinding;
import androidx.viewbinding.ViewBindings;
import com.acutecoder.pdfviewerdemo.R;
import java.lang.NullPointerException;
import java.lang.Override;
import java.lang.String;

public final class ActivityMainBinding implements ViewBinding {
  @NonNull
  private final LinearLayout rootView;

  @NonNull
  public final ImageButton btnBack;

  @NonNull
  public final Button btnElegirJson;

  @NonNull
  public final ImageButton btnHome;

  @NonNull
  public final LinearLayout container;

  @NonNull
  public final RecyclerView recyclerView;

  @NonNull
  public final Toolbar toolbarMain;

  private ActivityMainBinding(@NonNull LinearLayout rootView, @NonNull ImageButton btnBack,
      @NonNull Button btnElegirJson, @NonNull ImageButton btnHome, @NonNull LinearLayout container,
      @NonNull RecyclerView recyclerView, @NonNull Toolbar toolbarMain) {
    this.rootView = rootView;
    this.btnBack = btnBack;
    this.btnElegirJson = btnElegirJson;
    this.btnHome = btnHome;
    this.container = container;
    this.recyclerView = recyclerView;
    this.toolbarMain = toolbarMain;
  }

  @Override
  @NonNull
  public LinearLayout getRoot() {
    return rootView;
  }

  @NonNull
  public static ActivityMainBinding inflate(@NonNull LayoutInflater inflater) {
    return inflate(inflater, null, false);
  }

  @NonNull
  public static ActivityMainBinding inflate(@NonNull LayoutInflater inflater,
      @Nullable ViewGroup parent, boolean attachToParent) {
    View root = inflater.inflate(R.layout.activity_main, parent, false);
    if (attachToParent) {
      parent.addView(root);
    }
    return bind(root);
  }

  @NonNull
  public static ActivityMainBinding bind(@NonNull View rootView) {
    // The body of this method is generated in a way you would not otherwise write.
    // This is done to optimize the compiled bytecode for size and performance.
    int id;
    missingId: {
      id = R.id.btnBack;
      ImageButton btnBack = ViewBindings.findChildViewById(rootView, id);
      if (btnBack == null) {
        break missingId;
      }

      id = R.id.btnElegirJson;
      Button btnElegirJson = ViewBindings.findChildViewById(rootView, id);
      if (btnElegirJson == null) {
        break missingId;
      }

      id = R.id.btnHome;
      ImageButton btnHome = ViewBindings.findChildViewById(rootView, id);
      if (btnHome == null) {
        break missingId;
      }

      LinearLayout container = (LinearLayout) rootView;

      id = R.id.recyclerView;
      RecyclerView recyclerView = ViewBindings.findChildViewById(rootView, id);
      if (recyclerView == null) {
        break missingId;
      }

      id = R.id.toolbarMain;
      Toolbar toolbarMain = ViewBindings.findChildViewById(rootView, id);
      if (toolbarMain == null) {
        break missingId;
      }

      return new ActivityMainBinding((LinearLayout) rootView, btnBack, btnElegirJson, btnHome,
          container, recyclerView, toolbarMain);
    }
    String missingId = rootView.getResources().getResourceName(id);
    throw new NullPointerException("Missing required view with ID: ".concat(missingId));
  }
}
