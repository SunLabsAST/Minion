#!/usr/bin/perl

$new = 0;
while(<>) {

  chop();

  if($_ eq "") {
    if(!$new) {
      print "\n";
      $new = 1;
    }
  } else {
    $new = 0;
    print "$_\n";
  }
}
