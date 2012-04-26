package controllers;

import play.*;
import play.mvc.*;

import views.html.*;

import com.google.common.collect.ImmutableSet;
import org.jclouds.apis.Apis;

public class Application extends Controller {
  
  public static Result index() {
    return ok(index.render("available apis: " + ImmutableSet.copyOf(Apis.all())));
  }
  
}
