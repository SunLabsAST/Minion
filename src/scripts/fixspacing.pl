#!/usr/bin/perl
while(<>) {
  chop;
  $_ =~ s/^\s*//;
  $_ =~ s/\s*$//;
  if($_ eq "") {
    next;
  }
  @_ = split();
  push @words, @_;
}

print join("\n", @words), "\n";

