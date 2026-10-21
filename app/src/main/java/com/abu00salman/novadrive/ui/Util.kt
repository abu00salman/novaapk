package com.abu00salman.novadrive.ui

import android.content.Context
import android.widget.Toast

fun toast(ctx: Context, msg: String) = Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show()
