#!/usr/bin/perl

$last = 0;
while (<>) {
  chop;
 s:^[\W_]+::;
 s:[\W_]+: :g;
  if($_ eq "") {
    if(!$last) {
      print "\n";
      $last = 1;
    }
  } else {
    print "$_\n";
    $last = 0;
  }
}
